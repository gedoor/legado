package io.legado.app.ui.rss.source

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.utils.getViewModel


class RssSourceActivity : VMBaseActivity<RssSourceViewModel>(R.layout.activity_rss_source) {
    override val viewModel: RssSourceViewModel
        get() = getViewModel(RssSourceViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {

    }


}