package io.legado.app.ui.main.explore.style1

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.databinding.FragmentExplore1Binding
import io.legado.app.ui.main.explore.ExploreViewModel
import io.legado.app.utils.viewbindingdelegate.viewBinding

class ExploreFragment1 : VMBaseFragment<ExploreViewModel>(R.layout.fragment_explore1) {

    private val binding by viewBinding(FragmentExplore1Binding::bind)
    override val viewModel by viewModels<ExploreViewModel>()

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {

    }

}