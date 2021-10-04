package io.legado.app.ui.widget.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogCodeViewBinding
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding
import java.util.regex.Pattern

class CodeDialog() : BaseDialogFragment() {

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_code_view, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.codeView.addSyntaxPattern(Pattern.compile("[\",:;[->]{}()]"), accentColor)
        arguments?.getString("code")?.let {
            binding.codeView.setText(it)
        }
    }

}