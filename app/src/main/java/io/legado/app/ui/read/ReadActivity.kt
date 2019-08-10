package io.legado.app.ui.read

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_read.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ReadActivity : VMBaseActivity<ReadViewModel>(R.layout.activity_read) {
    override val viewModel: ReadViewModel
        get() = getViewModel(ReadViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        viewModel.initData(intent)
    }

    private fun initView() {
        tv_chapter_name.onClick {

        }
        tv_chapter_url.onClick {

        }
    }
}