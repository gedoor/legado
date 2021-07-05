package io.legado.app.ui.dict

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import io.legado.app.base.BaseDialogFragment

class DictDialog : BaseDialogFragment() {

    companion object {

        fun dict(manager: FragmentManager, word: String) {
            DictDialog().apply {
                val bundle = Bundle()
                bundle.putString("word", word)
                arguments = bundle
            }.show(manager, word)
        }

    }

    private val viewModel by viewModels<DictViewModel>()

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.word = arguments?.getString("word") ?: ""


    }


}