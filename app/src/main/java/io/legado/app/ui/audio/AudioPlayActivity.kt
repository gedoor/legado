package io.legado.app.ui.audio

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
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
import io.legado.app.help.BlurTransformation
import io.legado.app.help.ImageLoader
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.okButton
import io.legado.app.service.AudioPlayService
import io.legado.app.service.help.AudioPlay
import io.legado.app.ui.book.changesource.ChangeSourceDialog
import io.legado.app.ui.book.toc.ChapterListActivity
import io.legado.app.ui.widget.image.CoverImageView
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_audio_play.*
import org.apache.commons.lang3.time.DateFormatUtils
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.sdk27.listeners.onLongClick
import org.jetbrains.anko.startActivityForResult


class AudioPlayActivity :
    VMBaseActivity<AudioPlayViewModel>(R.layout.activity_audio_play, toolBarTheme = Theme.Dark),
    ChangeSourceDialog.CallBack {

    override val viewModel: AudioPlayViewModel
        get() = getViewModel(AudioPlayViewModel::class.java)

    private var requestCodeChapter = 8461
    private var adjustProgress = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        title_bar.transparent()
        AudioPlay.titleData.observe(this, { title_bar.title = it })
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
        fab_play_stop.onClick {
            playButton()
        }
        fab_play_stop.onLongClick {
            AudioPlay.stop(this)
            true
        }
        iv_skip_next.onClick {
            AudioPlay.next(this)
        }
        iv_skip_previous.onClick {
            AudioPlay.prev(this)
        }
        player_progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tv_dur_time.text = DateFormatUtils.format(progress.toLong(), "mm:ss")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                adjustProgress = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                adjustProgress = false
                AudioPlay.adjustProgress(this@AudioPlayActivity, player_progress.progress)
            }
        })
        iv_chapter.onClick {
            AudioPlay.book?.let {
                startActivityForResult<ChapterListActivity>(
                    requestCodeChapter,
                    Pair("bookUrl", it.bookUrl)
                )
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            iv_fast_rewind.invisible()
            iv_fast_forward.invisible()
        }
        iv_fast_forward.onClick {
            AudioPlay.adjustSpeed(this, 0.1f)
        }
        iv_fast_rewind.onClick {
            AudioPlay.adjustSpeed(this, -0.1f)
        }
    }

    private fun upCover(path: String?) {
        ImageLoader.load(this, path)
            .placeholder(CoverImageView.defaultDrawable)
            .error(CoverImageView.defaultDrawable)
            .into(iv_cover)
        ImageLoader.load(this, path)
            .transition(DrawableTransitionOptions.withCrossFade(1500))
            .thumbnail(defaultCover())
            .apply(bitmapTransform(BlurTransformation(this, 25)))
            .into(iv_bg)
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
                this.alert(title = getString(R.string.add_to_shelf)) {
                    message = getString(R.string.check_add_bookshelf, it.name)
                    okButton {
                        AudioPlay.inBookshelf = true
                        setResult(Activity.RESULT_OK)
                    }
                    noButton { viewModel.removeFromBookshelf { super.finish() } }
                }.show().applyTint()
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
                        val isPlay = !AudioPlayService.pause
                        AudioPlay.pause(this)
                        AudioPlay.status = Status.STOP
                        AudioPlay.durChapterIndex = it
                        AudioPlay.durPageIndex = 0
                        AudioPlay.book?.durChapterIndex = AudioPlay.durChapterIndex
                        viewModel.saveRead()
                        if (isPlay) {
                            AudioPlay.play(this)
                        }
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
                fab_play_stop.setImageResource(R.drawable.ic_pause_24dp)
            } else {
                fab_play_stop.setImageResource(R.drawable.ic_play_24dp)
            }
        }
        observeEventSticky<String>(EventBus.AUDIO_SUB_TITLE) {
            tv_sub_title.text = it
        }
        observeEventSticky<Int>(EventBus.AUDIO_SIZE) {
            player_progress.max = it
            tv_all_time.text = DateFormatUtils.format(it.toLong(), "mm:ss")
        }
        observeEventSticky<Int>(EventBus.AUDIO_PROGRESS) {
            AudioPlay.durPageIndex = it
            if (!adjustProgress) player_progress.progress = it
            tv_dur_time.text = DateFormatUtils.format(it.toLong(), "mm:ss")
        }
        observeEventSticky<Float>(EventBus.AUDIO_SPEED) {
            tv_speed.text = String.format("%.1fX", it)
            tv_speed.visible()
        }
    }

}