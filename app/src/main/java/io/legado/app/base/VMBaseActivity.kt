package io.legado.app.base

import androidx.lifecycle.ViewModel

abstract class VMBaseActivity<VM : ViewModel>(layoutID: Int, fullScreen: Boolean = true) :
    BaseActivity(layoutID, fullScreen) {

    protected abstract val viewModel: VM

}