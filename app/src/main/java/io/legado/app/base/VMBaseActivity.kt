package io.legado.app.base

import androidx.lifecycle.ViewModel

abstract class VMBaseActivity<VM : ViewModel>(
    layoutID: Int,
    fullScreen: Boolean = true,
    initTheme: Boolean = true
) : BaseActivity(layoutID, fullScreen, initTheme) {

    protected abstract val viewModel: VM

}