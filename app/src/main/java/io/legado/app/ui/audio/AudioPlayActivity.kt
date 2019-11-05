package io.legado.app.ui.audio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import androidx.lifecycle.Observer
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Bus
import io.legado.app.constant.Status
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BlurTransformation
import io.legado.app.help.ImageLoader
import io.legado.app.service.help.AudioPlay
import io.legado.app.ui.chapterlist.ChapterListActivity
import io.legado.app.utils.getViewModel
import io.legado.app.utils.observeEvent
import kotlinx.android.synthetic.main.activity_audio_play.*
import kotlinx.android.synthetic.main.view_title_bar.*
import org.apache.commons.lang3.time.DateFormatUtils
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.sdk27.listeners.onLongClick
import org.jetbrains.anko.startActivityForResult

class AudioPlayActivity : VMBaseActivity<AudioPlayViewModel>(R.layout.activity_audio_play),
    AudioPlayViewModel.CallBack {

    override val viewModel: AudioPlayViewModel
        get() = getViewModel(AudioPlayViewModel::class.java)

    private var requestCodeChapter = 8461
    private var adjustProgress = false
    private var status = Status.STOP

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setSupportActionBar(toolbar)
        viewModel.callBack = this
        viewModel.titleData.observe(this, Observer { title_bar.title = it })
        viewModel.coverData.observe(this, Observer { upCover(it) })
        viewModel.initData(intent)
        initView()
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
            viewModel.moveToNext()
        }
        iv_skip_previous.onClick {
            viewModel.moveToPrev()
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
            viewModel.book?.let {
                startActivityForResult<ChapterListActivity>(
                    requestCodeChapter,
                    Pair("bookUrl", it.bookUrl)
                )
            }
        }
    }

    private fun upCover(path: String) {
        ImageLoader.load(this, path)
            .placeholder(R.drawable.image_cover_default)
            .error(R.drawable.image_cover_default)
            .centerCrop()
            .setAsDrawable(iv_cover)
        ImageLoader.load(this, path)
            .placeholder(R.drawable.image_cover_default)
            .error(R.drawable.image_cover_default)
            .bitmapTransform(BlurTransformation(this, 25))
            .centerCrop()
            .setAsDrawable(iv_bg)
    }

    private fun playButton() {
        when (status) {
            Status.PLAY -> AudioPlay.pause(this)
            Status.PAUSE -> AudioPlay.resume(this)
            else -> viewModel.loadContent(viewModel.durChapterIndex)
        }
    }

    override fun contentLoadFinish(bookChapter: BookChapter, content: String) {
        AudioPlay.play(
            this,
            viewModel.book?.name,
            bookChapter.title,
            content,
            viewModel.durPageIndex
        )
        viewModel.loadContent(viewModel.durChapterIndex + 1)
    }

    override fun observeLiveBus() {
        observeEvent<Boolean>(Bus.AUDIO_PLAY_BUTTON) {
            playButton()
        }
        observeEvent<Int>(Bus.AUDIO_NEXT) {
            viewModel.moveToNext()
        }
        observeEvent<Int>(Bus.AUDIO_STATE) {
            status = it
            if (status == Status.PLAY) {
                fab_play_stop.setImageResource(R.drawable.ic_pause_24dp)
            } else {
                fab_play_stop.setImageResource(R.drawable.ic_play_24dp)
            }
        }
        observeEvent<Int>(Bus.AUDIO_PROGRESS) {
            if (!adjustProgress) player_progress.progress = it
            tv_dur_time.text = DateFormatUtils.format(it.toLong(), "mm:ss")
        }
        observeEvent<Int>(Bus.AUDIO_SIZE) {
            player_progress.max = it
            tv_all_time.text = DateFormatUtils.format(it.toLong(), "mm:ss")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                requestCodeChapter -> data?.getIntExtra("index", viewModel.durChapterIndex)?.let {
                    if (it != viewModel.durChapterIndex) {
                        viewModel.loadContent(it)
                    }
                }
            }
        }
    }
}