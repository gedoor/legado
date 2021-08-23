package io.legado.app.ui.book.login

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogLoginBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.text.EditText
import io.legado.app.ui.widget.text.TextInputLayout
import io.legado.app.utils.GSON
import io.legado.app.utils.applyTint
import io.legado.app.utils.viewbindingdelegate.viewBinding

class RuleUiLoginDialog : BaseDialogFragment() {

    private val binding by viewBinding(DialogLoginBinding::bind)
    private val viewModel by activityViewModels<SourceLoginViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_login, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        val bookSource = viewModel.bookSource ?: return
        val loginHeader = bookSource.getLoginHeader()
        val loginUi = bookSource.loginUi
        loginUi?.forEachIndexed { index, rowUi ->
            when (rowUi.type) {
                "text" -> layoutInflater.inflate(R.layout.item_source_edit, binding.root)
                    .apply {
                        id = index
                        findViewById<EditText>(R.id.editText).apply {
                            setText(loginHeader?.get(rowUi.name))
                        }
                    }
                "password" -> layoutInflater.inflate(R.layout.item_source_edit, binding.root)
                    .apply {
                        id = index
                        findViewById<EditText>(R.id.editText).apply {
                            inputType =
                                InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
                            setText(loginHeader?.get(rowUi.name))
                        }
                    }
            }
        }
        binding.toolBar.inflateMenu(R.menu.source_login)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_ok -> {
                    val loginData = hashMapOf<String, String?>()
                    loginUi?.forEachIndexed { index, rowUi ->
                        when (rowUi.type) {
                            "text", "password" -> {
                                val value = binding.root.findViewById<TextInputLayout>(index)
                                    .findViewById<EditText>(R.id.editText).text?.toString()
                                loginData[rowUi.name] = value
                            }
                        }
                    }
                    bookSource.putLoginInfo(GSON.toJson(loginData))
                }
            }
            return@setOnMenuItemClickListener true
        }
    }

}