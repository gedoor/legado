package io.legado.app.ui.importbook

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.utils.getViewModel


class ImportBookActivity : VMBaseActivity<ImportBookViewModel>(R.layout.activity_import_book) {

    override val viewModel: ImportBookViewModel
        get() = getViewModel(ImportBookViewModel::class.java)


    override fun onActivityCreated(savedInstanceState: Bundle?) {

    }

}