package io.legado.app.ui.rss.read

import android.os.Bundle
import androidx.lifecycle.Observer
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_rss_read.*

class ReadRssActivity : VMBaseActivity<ReadRssViewModel>(R.layout.activity_rss_read) {

    override val viewModel: ReadRssViewModel
        get() = getViewModel(ReadRssViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initLiveData()
        viewModel.initData(intent)

    }

    private fun initLiveData() {
        viewModel.contentLiveData.observe(this, Observer {
            webView.loadData("<style>img{max-width:100%}</style>$it", "text/html", "utf-8")
        })
        viewModel.urlLiveData.observe(this, Observer {
            webView.loadUrl(it)
        })
    }

}