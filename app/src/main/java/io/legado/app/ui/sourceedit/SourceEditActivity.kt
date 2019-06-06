package io.legado.app.ui.sourceedit

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.utils.getViewModel

class SourceEditActivity : BaseActivity<SourceEditViewModel>() {
    override val viewModel: SourceEditViewModel
        get() = getViewModel(SourceEditViewModel::class.java)
    override val layoutID: Int
        get() = R.layout.activity_source_edit

    override fun onViewModelCreated(viewModel: SourceEditViewModel, savedInstanceState: Bundle?) {

    }


}