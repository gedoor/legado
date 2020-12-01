package io.legado.app.base

import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding
import io.legado.app.constant.Theme

abstract class VMBaseActivity<VB : ViewBinding, VM : ViewModel>(
    fullScreen: Boolean = true,
    theme: Theme = Theme.Auto,
    toolBarTheme: Theme = Theme.Auto
) : BaseActivity<VB>(fullScreen, theme, toolBarTheme) {

    protected abstract val viewModel: VM

}