package io.legado.app.ui.audio

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.BookChapter
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.view_title_bar.*

class AudioPlayActivity : VMBaseActivity<AudioPlayViewModel>(R.layout.activity_audio_play),
    AudioPlayViewModel.CallBack {

    override val viewModel: AudioPlayViewModel
        get() = getViewModel(AudioPlayViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setSupportActionBar(toolbar)
        viewModel.initData(intent)
    }

    override fun loadContent() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun contentLoadFinish(bookChapter: BookChapter, content: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun upContent() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}