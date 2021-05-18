package io.legado.app.ui.audio

import android.app.Activity
import android.graphics.drawable.Drawable
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import androidx.activity.viewModels
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
import io.legado.app.ui.book.toc.TocActivityResult
import io.legado.app.ui.widget.image.CoverImageView
import io.legado.app.ui.widget.seekbar.SeekBarChangeListener
import io.legado.app.utils.*
import splitties.views.onLongClick
import java.util.*

/**
 * 音频播放
 */
class AudioPlayActivity :
    VMBaseActivity<ActivityAudioPlayBinding, AudioPlayViewModel>(toolBarTheme = Theme.Dark),
    ChangeSourceDialog.CallBack {

    override val viewModel: AudioPlayViewModel
            by viewModels()

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
            if (it.first != AudioPlay.durChapterIndex) {
                AudioPlay.skipTo(this, it.first)
            }
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
            AudioPlay.addTimer(this@AudioPlayActivity)
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
                    setMessage(getString(R.string.check_add_bookshelf, it.name))
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