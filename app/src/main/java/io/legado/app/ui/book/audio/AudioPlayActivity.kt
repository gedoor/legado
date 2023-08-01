package io.legado.app.ui.book.audio

import android.annotation.SuppressLint
import android.app.Activity
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.BookType
import io.legado.app.constant.EventBus
import io.legado.app.constant.Status
import io.legado.app.constant.Theme
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.databinding.ActivityAudioPlayBinding
import io.legado.app.help.book.isAudio
import io.legado.app.help.book.removeType
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.model.AudioPlay
import io.legado.app.model.BookCover
import io.legado.app.service.AudioPlayService
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.book.changesource.ChangeBookSourceDialog
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.book.toc.TocActivityResult
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.widget.seekbar.SeekBarChangeListener
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.views.onLongClick
import java.util.*

/**
 * 音频播放
 */
@SuppressLint("ObsoleteSdkInt")
class AudioPlayActivity :
    VMBaseActivity<ActivityAudioPlayBinding, AudioPlayViewModel>(toolBarTheme = Theme.Dark),
    ChangeBookSourceDialog.CallBack {

    override val binding by viewBinding(ActivityAudioPlayBinding::inflate)
    override val viewModel by viewModels<AudioPlayViewModel>()
    private val timerSliderPopup by lazy { TimerSliderPopup(this) }
    private var adjustProgress = false

    private val progressTimeFormat by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SimpleDateFormat("mm:ss", Locale.getDefault())
        } else {
            java.text.SimpleDateFormat("mm:ss", Locale.getDefault())
        }
    }
    private val tocActivityResult = registerForActivityResult(TocActivityResult()) {
        it?.let {
            if (it.first != AudioPlay.book?.durChapterIndex
                || it.second == 0
            ) {
                AudioPlay.skipTo(this, it.first)
            }
        }
    }
    private val sourceEditResult =
        registerForActivityResult(StartActivityContract(BookSourceEditActivity::class.java)) {
            if (it.resultCode == RESULT_OK) {
                viewModel.upSource()
            }
        }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.setBackgroundResource(R.color.transparent)
        AudioPlay.titleData.observe(this) {
            binding.titleBar.title = it
        }
        AudioPlay.coverData.observe(this) {
            upCover(it)
        }
        viewModel.initData(intent)
        initView()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.audio_play, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_login)?.isVisible = !AudioPlay.bookSource?.loginUrl.isNullOrBlank()
        menu.findItem(R.id.menu_wake_lock)?.isChecked = AppConfig.audioPlayUseWakeLock
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_change_source -> AudioPlay.book?.let {
                showDialogFragment(ChangeBookSourceDialog(it.name, it.author))
            }
            R.id.menu_login -> AudioPlay.bookSource?.let {
                startActivity<SourceLoginActivity> {
                    putExtra("type", "bookSource")
                    putExtra("key", it.bookSourceUrl)
                }
            }
            R.id.menu_wake_lock -> AppConfig.audioPlayUseWakeLock = !AppConfig.audioPlayUseWakeLock
            R.id.menu_copy_audio_url -> sendToClip(AudioPlayService.url)
            R.id.menu_edit_source -> AudioPlay.bookSource?.let {
                sourceEditResult.launch {
                    putExtra("sourceUrl", it.bookSourceUrl)
                }
            }
            R.id.menu_log -> showDialogFragment<AppLogDialog>()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        binding.fabPlayStop.setOnClickListener {
            playButton()
        }
        binding.fabPlayStop.onLongClick {
            AudioPlay.stop(this@AudioPlayActivity)
        }
        binding.ivSkipNext.setOnClickListener {
            AudioPlay.next(this@AudioPlayActivity)
        }
        binding.ivSkipPrevious.setOnClickListener {
            AudioPlay.prev(this@AudioPlayActivity)
        }
        binding.playerProgress.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvDurTime.text = progressTimeFormat.format(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                adjustProgress = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                adjustProgress = false
                AudioPlay.adjustProgress(this@AudioPlayActivity, seekBar.progress)
            }
        })
        binding.ivChapter.setOnClickListener {
            AudioPlay.book?.let {
                tocActivityResult.launch(it.bookUrl)
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            binding.ivFastRewind.invisible()
            binding.ivFastForward.invisible()
        }
        binding.ivFastForward.setOnClickListener {
            AudioPlay.adjustSpeed(this@AudioPlayActivity, 0.1f)
        }
        binding.ivFastRewind.setOnClickListener {
            AudioPlay.adjustSpeed(this@AudioPlayActivity, -0.1f)
        }
        binding.ivTimer.setOnClickListener {
            timerSliderPopup.showAsDropDown(it, 0, (-100).dpToPx(), Gravity.TOP)
        }
    }

    private fun upCover(path: String?) {
        BookCover.load(this, path, sourceOrigin = AudioPlay.bookSource?.bookSourceUrl)
            .into(binding.ivCover)
        BookCover.loadBlur(this, path)
            .into(binding.ivBg)
    }

    private fun playButton() {
        when (AudioPlay.status) {
            Status.PLAY -> AudioPlay.pause(this)
            Status.PAUSE -> AudioPlay.resume(this)
            else -> AudioPlay.play(this)
        }
    }

    override val oldBook: Book?
        get() = AudioPlay.book

    override fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        if (book.isAudio) {
            viewModel.changeTo(source, book, toc)
        } else {
            AudioPlay.stop(this)
            lifecycleScope.launch {
                withContext(IO) {
                    AudioPlay.book?.migrateTo(book, toc)
                    book.removeType(BookType.updateError)
                    AudioPlay.book?.delete()
                    appDb.bookDao.insert(book)
                }
                startActivity<ReadBookActivity> {
                    putExtra("bookUrl", book.bookUrl)
                }
                finish()
            }
        }
    }

    override fun finish() {
        AudioPlay.book?.let {
            if (!AudioPlay.inBookshelf) {
                if (!AppConfig.showAddToShelfAlert) {
                    viewModel.removeFromBookshelf { super.finish() }
                } else {
                    alert(title = getString(R.string.add_to_bookshelf)) {
                        setMessage(getString(R.string.check_add_bookshelf, it.name))
                        okButton {
                            AudioPlay.inBookshelf = true
                            setResult(Activity.RESULT_OK)
                        }
                        noButton { viewModel.removeFromBookshelf { super.finish() } }
                    }
                }
            } else {
                super.finish()
            }
        } ?: super.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (AudioPlay.status != Status.PLAY) {
            AudioPlay.stop(this)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun observeLiveBus() {
        observeEvent<Boolean>(EventBus.MEDIA_BUTTON) {
            if (it) {
                playButton()
            }
        }
        observeEventSticky<Int>(EventBus.AUDIO_STATE) {
            AudioPlay.status = it
            if (it == Status.PLAY) {
                binding.fabPlayStop.setImageResource(R.drawable.ic_pause_24dp)
            } else {
                binding.fabPlayStop.setImageResource(R.drawable.ic_play_24dp)
            }
        }
        observeEventSticky<String>(EventBus.AUDIO_SUB_TITLE) {
            binding.tvSubTitle.text = it
            AudioPlay.book?.let { book ->
                binding.ivSkipPrevious.isEnabled = book.durChapterIndex > 0
                binding.ivSkipNext.isEnabled = book.durChapterIndex < book.totalChapterNum - 1
            }
        }
        observeEventSticky<Int>(EventBus.AUDIO_SIZE) {
            binding.playerProgress.max = it
            binding.tvAllTime.text = progressTimeFormat.format(it.toLong())
        }
        observeEventSticky<Int>(EventBus.AUDIO_PROGRESS) {
            if (!adjustProgress) binding.playerProgress.progress = it
            binding.tvDurTime.text = progressTimeFormat.format(it.toLong())
        }
        observeEventSticky<Int>(EventBus.AUDIO_BUFFER_PROGRESS) {
            binding.playerProgress.secondaryProgress = it

        }
        observeEventSticky<Float>(EventBus.AUDIO_SPEED) {
            binding.tvSpeed.text = String.format("%.1fX", it)
            binding.tvSpeed.visible()
        }
        observeEventSticky<Int>(EventBus.AUDIO_DS) {
            binding.tvTimer.text = "${it}m"
            binding.tvTimer.visible(it > 0)
        }
    }

}