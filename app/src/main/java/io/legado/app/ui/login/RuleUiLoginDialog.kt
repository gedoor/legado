package io.legado.app.ui.login

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.fragment.app.activityViewModels
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.AppLog
import io.legado.app.data.entities.BaseSource
import io.legado.app.databinding.DialogLoginBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.text.EditText
import io.legado.app.ui.widget.text.TextInputLayout
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.views.onClick

class RuleUiLoginDialog : BaseDialogFragment() {

    private val binding by viewBinding(DialogLoginBinding::bind)
    private val viewModel by activityViewModels<SourceLoginViewModel>()

    override fun onStart() {
        super.onStart()
        setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_login, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val source = viewModel.source ?: return
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.title = getString(R.string.login_source, source.getTag())
        val loginInfo = source.getLoginInfoMap()
        val loginUi = source.loginUi
        loginUi?.forEachIndexed { index, rowUi ->
            when (rowUi.type) {
                "text" -> layoutInflater.inflate(R.layout.item_source_edit, binding.root, false)
                    .let {
                        binding.flexbox.addView(it)
                        it.id = index
                        (it as TextInputLayout).hint = rowUi.name
                        it.findViewById<EditText>(R.id.editText).apply {
                            setText(loginInfo?.get(rowUi.name))
                        }
                    }
                "password" -> layoutInflater.inflate(R.layout.item_source_edit, binding.root, false)
                    .let {
                        binding.flexbox.addView(it)
                        it.id = index
                        (it as TextInputLayout).hint = rowUi.name
                        it.findViewById<EditText>(R.id.editText).apply {
                            inputType =
                                InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
                            setText(loginInfo?.get(rowUi.name))
                        }
                    }
                "button" -> layoutInflater.inflate(R.layout.item_fillet_text, binding.root, false)
                    .let {
                        binding.flexbox.addView(it)
                        it.id = index
                        (it as TextView).let { textView ->
                            textView.text = rowUi.name
                            textView.setPadding(16.dp)
                        }
                        it.onClick {
                            if (rowUi.action.isAbsUrl()) {
                                context?.openUrl(rowUi.action!!)
                            }
                        }
                    }
            }
        }
        binding.toolBar.inflateMenu(R.menu.source_login)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_ok -> {
                    val loginData = hashMapOf<String, String>()
                    loginUi?.forEachIndexed { index, rowUi ->
                        when (rowUi.type) {
                            "text", "password" -> {
                                val value = binding.root.findViewById<TextInputLayout>(index)
                                    .findViewById<EditText>(R.id.editText).text?.toString()
                                if (!value.isNullOrBlank()) {
                                    loginData[rowUi.name] = value
                                }
                            }
                        }
                    }
                    login(source, loginData)
                }
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun login(source: BaseSource, loginData: HashMap<String, String>) {
        launch(IO) {
            if (loginData.isEmpty()) {
                source.removeLoginInfo()
                withContext(Main) {
                    dismiss()
                }
            } else if (source.putLoginInfo(GSON.toJson(loginData))) {
                source.getLoginJs()?.let {
                    try {
                        source.evalJS(it)
                        toastOnUi(R.string.success)
                        withContext(Main) {
                            dismiss()
                        }
                    } catch (e: Exception) {
                        AppLog.put("登录出错\n${e.localizedMessage}", e)
                        toastOnUi("error:${e.localizedMessage}")
                        e.printOnDebug()
                    }
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        activity?.finish()
    }

}