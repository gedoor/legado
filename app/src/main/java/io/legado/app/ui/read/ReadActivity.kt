package io.legado.app.ui.read

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.utils.getViewModel

class ReadActivity : VMBaseActivity<ReadViewModel>(R.layout.activity_read) {
    override val viewModel: ReadViewModel
        get() = getViewModel(ReadViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {

    }


}