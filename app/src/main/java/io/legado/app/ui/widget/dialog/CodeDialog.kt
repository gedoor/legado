package io.legado.app.ui.widget.dialog

import android.os.Bundle
import android.view.View
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogCodeViewBinding
import io.legado.app.utils.addJsPattern
import io.legado.app.utils.addJsonPattern
import io.legado.app.utils.addLegadoPattern
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding

class CodeDialog() : BaseDialogFragment(R.layout.dialog_code_view) {

    constructor(code: String) : this() {
        arguments = Bundle().apply {
            putString("code", code)
        }
    }

    val binding by viewBinding(DialogCodeViewBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.9f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.codeView.addLegadoPattern()
        binding.codeView.addJsonPattern()
        binding.codeView.addJsPattern()
        arguments?.getString("code")?.let {
            binding.codeView.setText(it)
        }
    }

}