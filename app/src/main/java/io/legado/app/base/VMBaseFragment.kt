package io.legado.app.base

import androidx.lifecycle.ViewModel

abstract class VMBaseFragment<VM : ViewModel>(layoutID: Int) : BaseFragment(layoutID) {

    protected abstract val viewModel: VM

}
