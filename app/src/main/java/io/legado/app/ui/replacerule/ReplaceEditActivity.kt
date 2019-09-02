package io.legado.app.ui.replacerule

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.utils.getViewModel

class ReplaceEditActivity :
    VMBaseActivity<ReplaceEditViewModel>(R.layout.activity_replace_edit, false) {
    override val viewModel: ReplaceEditViewModel
        get() = getViewModel(ReplaceEditViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {

    }


}
