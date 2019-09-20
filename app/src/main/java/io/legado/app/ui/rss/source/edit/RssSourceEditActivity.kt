package io.legado.app.ui.rss.source.edit

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.utils.getViewModel

class RssSourceEditActivity :
    VMBaseActivity<RssSourceEditViewModel>(R.layout.activity_rss_source_edit) {

    override val viewModel: RssSourceEditViewModel
        get() = getViewModel(RssSourceEditViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {

    }


}