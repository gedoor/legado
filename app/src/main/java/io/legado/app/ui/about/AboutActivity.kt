package io.legado.app.ui.about

import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.utils.getViewModel

class AboutActivity : BaseActivity<AndroidViewModel>() {
    override val viewModel: AndroidViewModel
        get() = getViewModel(AndroidViewModel::class.java)
    override val layoutID: Int
        get() = R.layout.activity_about

    override fun onViewModelCreated(viewModel: AndroidViewModel, savedInstanceState: Bundle?) {
        super.onViewModelCreated(viewModel, savedInstanceState)
    }

}
