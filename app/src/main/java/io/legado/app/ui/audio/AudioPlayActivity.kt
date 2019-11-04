package io.legado.app.ui.audio

import android.os.Bundle
import android.widget.SeekBar
import androidx.lifecycle.Observer
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Bus
import io.legado.app.constant.Status
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.ImageLoader
import io.legado.app.service.help.AudioPlay
import io.legado.app.utils.getViewModel
import io.legado.app.utils.observeEvent
import kotlinx.android.synthetic.main.activity_audio_play.*
import kotlinx.android.synthetic.main.view_title_bar.*
import org.jetbrains.anko.sdk27.listeners.onClick

class AudioPlayActivity : VMBaseActivity<AudioPlayViewModel>(R.layout.activity_audio_play),
    AudioPlayViewModel.CallBack {

    override val viewModel: AudioPlayViewModel
        get() = getViewModel(AudioPlayViewModel::class.java)

    private var status = Status.STOP

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setSupportActionBar(toolbar)
        viewModel.callBack = this
        viewModel.bookData.observe(this, Observer { upView(it) })
        viewModel.initData(intent)
        initView()
    }

    private fun initView() {
        fab_play_stop.onClick {
            when (status) {
                Status.PLAY -> AudioPlay.pause(this)
                Status.PAUSE -> AudioPlay.resume(this)
                else -> viewModel.bookData.value?.let {
                    viewModel.loadContent(it, viewModel.durChapterIndex)
                }
            }
        }
        iv_skip_next.onClick {

        }
        iv_skip_previous.onClick {

        }
        player_progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                AudioPlay.upProgress(this@AudioPlayActivity, player_progress.progress)
            }
        })
    }

    private fun upView(book: Book) {
        actionBar?.title = book.name
        ImageLoader.load(this, book.getDisplayCover())
            .placeholder(R.drawable.image_cover_default)
            .error(R.drawable.image_cover_default)
            .centerCrop()
            .setAsDrawable(iv_cover)
    }

    override fun contentLoadFinish(bookChapter: BookChapter, content: String) {
        AudioPlay.play(
            this,
            viewModel.bookData.value?.name,
            bookChapter.title,
            content,
            viewModel.durPageIndex
        )
        viewModel.bookData.value?.let {
            viewModel.loadContent(it, viewModel.durChapterIndex + 1)
        }
    }

    override fun observeLiveBus() {
        observeEvent<Int>(Bus.AUDIO_NEXT) {
            viewModel.durChapterIndex = viewModel.durChapterIndex + 1
            viewModel.bookData.value?.let {
                viewModel.loadContent(it, viewModel.durChapterIndex)
            }
        }
        observeEvent<Int>(Bus.AUDIO_STATE) {
            status = it
        }
        observeEvent<Int>(Bus.AUDIO_PROGRESS) {
            if (!player_progress.isInTouchMode)
                player_progress.progress = it
        }
    }
}