package io.legado.app.ui.audio

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.EventBus
import io.legado.app.constant.Status
import io.legado.app.constant.Theme
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ActivityAudioPlayBinding
import io.legado.app.help.BlurTransformation
import io.legado.app.help.ImageLoader
import io.legado.app.lib.dialogs.alert
import io.legado.app.service.help.AudioPlay
import io.legado.app.ui.book.changesource.ChangeSourceDialog
import io.legado.app.ui.book.toc.ChapterListActivity
import io.legado.app.ui.widget.image.CoverImageView
import io.legado.app.ui.widget.seekbar.SeekBarChangeListener
import io.legado.app.utils.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.sdk27.listeners.onLongClick
import org.jetbrains.anko.startActivityForResult
import java.util.*

/**
 * 音频播放
 */
class AudioPlayActivity :
    VMBaseActivity<ActivityAudioPlayBinding, AudioPlayViewModel>(toolBarTheme = Theme.Dark),
    ChangeSourceDialog.CallBack {

    override val viewModel: AudioPlayViewModel
        get() = getViewModel(AudioPlayViewModel::class.java)

    private var requestCodeChapter = 8461
    private var adjustProgress = false
    private val progressTimeFormat by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SimpleDateFormat("mm:ss", Locale.getDefault())
        } else {
            java.text.SimpleDateFormat("mm:ss", Locale.getDefault())
        }
    }

    override fun getViewBinding(): ActivityAudioPlayBinding {
        return ActivityAudioPlayBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.transparent()
        AudioPlay.titleData.observe(this, { binding.titleBar.title = it })
        AudioPlay.coverData.observe(this, { upCover(it) })
        viewModel.initData(intent)
        initView()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.audio_play, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_change_source -> AudioPlay.book?.let {
                ChangeSourceDialog.show(supportFragmentManager, it.name, it.author)
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        binding.fabPlayStop.onClick {
            playButton()
        }
        binding.fabPlayStop.onLongClick {
            AudioPlay.stop(this)
            true
        }
        binding.ivSkipNext.onClick {
            AudioPlay.next(this)
        }
        binding.ivSkipPrevious.onClick {
            AudioPlay.prev(this)
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
        binding.ivChapter.onClick {
            AudioPlay.book?.let {
                startActivityForResult<ChapterListActivity>(
                    requestCodeChapter,
                    Pair("bookUrl", it.bookUrl)
                )
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            binding.ivFastRewind.invisible()
            binding.ivFastForward.invisible()
        }
        binding.ivFastForward.onClick {
            AudioPlay.adjustSpeed(this, 0.1f)
        }
        binding.ivFastRewind.onClick {
            AudioPlay.adjustSpeed(this, -0.1f)
        }
        binding.ivTimer.onClick {
            AudioPlay.addTimer(this)
        }
    }

    private fun upCover(path: String?) {
        ImageLoader.load(this, path)
            .placeholder(CoverImageView.defaultDrawable)
            .error(CoverImageView.defaultDrawable)
            .into(binding.ivCover)
        ImageLoader.load(this, path)
            .transition(DrawableTransitionOptions.withCrossFade(1500))
            .thumbnail(defaultCover())
            .apply(bitmapTransform(BlurTransformation(this, 25)))
            .into(binding.ivBg)
    }

    private fun defaultCover(): RequestBuilder<Drawable> {
        return ImageLoader.load(this, CoverImageView.defaultDrawable)
            .apply(bitmapTransform(BlurTransformation(this, 25)))
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

    override fun changeTo(book: Book) {
        viewModel.changeTo(book)
    }

    override fun finish() {
        AudioPlay.book?.let {
            if (!AudioPlay.inBookshelf) {
                alert(title = getString(R.string.add_to_shelf)) {
                    message = getString(R.string.check_add_bookshelf, it.name)
                    okButton {
                        AudioPlay.inBookshelf = true
                        setResult(Activity.RESULT_OK)
                    }
                    noButton { viewModel.removeFromBookshelf { super.finish() } }
                }.show()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                requestCodeChapter -> data?.getIntExtra("index", AudioPlay.durChapterIndex)?.let {
                    if (it != AudioPlay.durChapterIndex) {
                        AudioPlay.skipTo(this, it)
                    }
                }
            }
        }
    }

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
        }
        observeEventSticky<Int>(EventBus.AUDIO_SIZE) {
            binding.playerProgress.max = it
            binding.tvAllTime.text = progressTimeFormat.format(it.toLong())
        }
        observeEventSticky<Int>(EventBus.AUDIO_PROGRESS) {
            AudioPlay.durChapterPos = it
            if (!adjustProgress) binding.playerProgress.progress = it
            binding.tvDurTime.text = progressTimeFormat.format(it.toLong())
        }
        observeEventSticky<Float>(EventBus.AUDIO_SPEED) {
            binding.tvSpeed.text = String.format("%.1fX", it)
            binding.tvSpeed.visible()
        }
    }

}