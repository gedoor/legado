package io.legado.app.ui.login

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import androidx.core.view.setPadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.script.rhino.runScriptWithContext
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.AppLog
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.rule.RowUi
import io.legado.app.databinding.DialogLoginBinding
import io.legado.app.databinding.ItemFilletTextBinding
import io.legado.app.databinding.ItemSourceEditBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.utils.GSON
import io.legado.app.utils.applyTint
import io.legado.app.utils.dpToPx
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.isAbsUrl
import io.legado.app.utils.openUrl
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.sendToClip
import io.legado.app.utils.setLayout
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import kotlin.text.lastIndexOf
import kotlin.text.startsWith
import kotlin.text.substring
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity


class SourceLoginDialog : BaseDialogFragment(R.layout.dialog_login, true) {

    private val binding by viewBinding(DialogLoginBinding::bind)
    private val viewModel by activityViewModels<SourceLoginViewModel>()
    private var lastClickTime: Long = 0
    private var oKToClose = false
    private var rowUis: List<RowUi>? = null
    private val sourceLoginJsExtensions by lazy { SourceLoginJsExtensions(activity as AppCompatActivity, viewModel.source) }

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    suspend fun evalUiJs(jsStr: String): String? = withContext(IO) {
        val source = viewModel.source ?: return@withContext null
        val loginJS = source.getLoginJs() ?: ""
        try {
            source.evalJS("$loginJS\n$jsStr") {
                put("result", viewModel.loginInfo)
                put("book", viewModel.book)
                put("chapter", viewModel.chapter)
            }.toString()
        } catch (e: Exception) {
            AppLog.put(source.getTag() + " loginUi err:" + (e.localizedMessage ?: e.toString()), e)
            null
        }
    }

    fun loginUi(json: String?): List<RowUi>? {
        return GSON.fromJsonArray<RowUi>(json).onFailure {
            it.printOnDebug()
        }.getOrNull()
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun buttonUi(source: BaseSource, rowUis: List<RowUi>?) {
        val loginInfo = viewModel.loginInfo
        rowUis?.forEachIndexed { index, rowUi ->
            when (rowUi.type) {
                RowUi.Type.text -> ItemSourceEditBinding.inflate(
                    layoutInflater,
                    binding.root,
                    false
                ).let {
                    binding.flexbox.addView(it.root)
                    it.root.id = index + 1000
                    it.textInputLayout.hint = rowUi.name
                    it.editText.setText(loginInfo[rowUi.name])
                }

                RowUi.Type.password -> ItemSourceEditBinding.inflate(
                    layoutInflater,
                    binding.root,
                    false
                ).let {
                    binding.flexbox.addView(it.root)
                    it.root.id = index + 1000
                    it.textInputLayout.hint = rowUi.name
                    it.editText.inputType =
                        InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
                    it.editText.setText(loginInfo[rowUi.name])
                }

                RowUi.Type.button -> ItemFilletTextBinding.inflate(
                    layoutInflater,
                    binding.root,
                    false
                ).let {
                    binding.flexbox.addView(it.root)
                    rowUi.style().apply(it.root)
                    it.root.id = index + 1000
                    it.textView.text = rowUi.name
                    rowUi.viewName?.let { jsStr ->
                        execute {
                            evalUiJs(jsStr)
                        }.onSuccess { name -> it.textView.text = name }
                    }
                    it.textView.setPadding(16.dpToPx())
                    var downTime = 0L
                    it.root.setOnTouchListener { view, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                view.isSelected = true
                                downTime = System.currentTimeMillis()
                            }
                            MotionEvent.ACTION_UP -> {
                                view.isSelected = false
                                val upTime = System.currentTimeMillis()
                                if (upTime - lastClickTime < 200) {
                                    return@setOnTouchListener true
                                }
                                lastClickTime = upTime
                                handleButtonClick(source, rowUi, rowUis, upTime > downTime + 666)
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                view.isSelected = false
                            }
                        }
                        return@setOnTouchListener true
                    }
                }

                RowUi.Type.toggle -> ItemFilletTextBinding.inflate(
                    layoutInflater,
                    binding.root,
                    false
                ).let {
                    binding.flexbox.addView(it.root)
                    rowUi.style().apply(it.root)
                    it.root.id = index + 1000
                    val chars = rowUi.chars ?: arrayOf("chars is null")
                    var char = loginInfo[rowUi.name]?.takeIf { it -> it.isNotEmpty() } ?: rowUi.default ?: chars.getOrNull(0) ?: "chars is []"
                    rowUi.default = char
                    it.textView.text = char + rowUi.name
                    rowUi.viewName?.let { jsStr ->
                        execute {
                            evalUiJs(jsStr)
                        }.onSuccess { name -> it.textView.text = char + name }
                    }
                    it.textView.setPadding(16.dpToPx())
                    var downTime = 0L
                    it.root.setOnTouchListener { view, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                view.isSelected = true
                                downTime = System.currentTimeMillis()
                            }
                            MotionEvent.ACTION_UP -> {
                                view.isSelected = false
                                val upTime = System.currentTimeMillis()
                                if (upTime - lastClickTime < 200) {
                                    return@setOnTouchListener true
                                }
                                lastClickTime = upTime
                                val currentIndex = chars.indexOf(char)
                                if (currentIndex == -1) {
                                    char = chars.getOrNull(0) ?: ""
                                    rowUi.default = char
                                    it.textView.text = char + rowUi.name
                                }
                                else {
                                    val nextIndex = (currentIndex + 1) % chars.size
                                    char = chars.getOrNull(nextIndex) ?: ""
                                    rowUi.default = char
                                    it.textView.text = char + rowUi.name
                                }
                                handleButtonClick(source, rowUi, rowUis, upTime > downTime + 666)
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                view.isSelected = false
                            }
                        }
                        return@setOnTouchListener true
                    }
                }
            }
        }
        binding.toolBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_ok -> {
                    oKToClose = true
                    val loginData = getLoginData(rowUis)
                    login(source, loginData)
                }

                R.id.menu_show_login_header -> alert {
                    setTitle(R.string.login_header)
                    source.getLoginHeader()?.let { loginHeader ->
                        setMessage(loginHeader)
                        positiveButton(R.string.copy_text) {
                            appCtx.sendToClip(loginHeader)
                        }
                    }
                }

                R.id.menu_del_login_header -> source.removeLoginHeader()
                R.id.menu_log -> showDialogFragment<AppLogDialog>()
            }
            return@setOnMenuItemClickListener true
        }
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val source = viewModel.source ?: return
        val loginUiStr = source.loginUi ?: return
        val jsCode = loginUiStr.let {
            when {
                it.startsWith("@js:") -> it.substring(4)
                it.startsWith("<js>") -> it.substring(4, it.lastIndexOf("<"))
                else -> null
            }
        }
        if (jsCode != null) {
            lifecycleScope.launch(Main) {
                rowUis = loginUi(evalUiJs(jsCode))
                buttonUi(source, rowUis)
            }
        }
        else {
            rowUis = loginUi(loginUiStr)
            buttonUi(source, rowUis)
        }
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.title = getString(R.string.login_source, source.getTag())
        binding.toolBar.inflateMenu(R.menu.source_login)
        binding.toolBar.menu.applyTint(requireContext())
    }

    private fun handleButtonClick(source: BaseSource, rowUi: RowUi, rowUis: List<RowUi>, isLongClick: Boolean) {
        lifecycleScope.launch(IO) {
            if (rowUi.action.isAbsUrl()) {
                context?.openUrl(rowUi.action!!)
            } else if (rowUi.action != null) {
                // JavaScript
                val buttonFunctionJS = rowUi.action!!
                val loginJS = source.getLoginJs() ?: return@launch
                kotlin.runCatching {
                    runScriptWithContext {
                        source.evalJS("$loginJS\n$buttonFunctionJS") {
                            put("java", sourceLoginJsExtensions)
                            put("result", getLoginData(rowUis))
                            put("book", viewModel.book)
                            put("chapter", viewModel.chapter)
                            put("isLongClick", isLongClick)
                        }
                    }
                }.onFailure { e ->
                    ensureActive()
                    AppLog.put("LoginUI Button ${rowUi.name} JavaScript error", e)
                }
            }
        }
    }

    private fun getLoginData(rowUis: List<RowUi>?): HashMap<String, String> {
        val loginData = hashMapOf<String, String>()
        rowUis?.forEachIndexed { index, rowUi ->
            when (rowUi.type) {
                "text", "password" -> {
                    val rowView = binding.root.findViewById<View>(index + 1000)
                    ItemSourceEditBinding.bind(rowView).editText.text.let {
                        loginData[rowUi.name] = it?.toString() ?: rowUi.default ?: "" //没文本的时候存空字符串,而不是删除loginInfo
                    }
                }
                "toggle" -> {
                    loginData[rowUi.name] = rowUi.default.toString()
                }
            }
        }
        return loginData
    }

    private fun login(source: BaseSource, loginData: HashMap<String, String>) {
        lifecycleScope.launch(IO) {
            if (loginData.isEmpty()) {
                source.removeLoginInfo()
                withContext(Main) {
                    dismiss()
                }
            } else if (source.putLoginInfo(GSON.toJson(loginData))) {
                try {
                    runScriptWithContext {
                        source.login()
                    }
                    context?.toastOnUi(R.string.success)
                    withContext(Main) {
                        dismiss()
                    }
                } catch (e: Exception) {
                    AppLog.put("登录出错\n${e.localizedMessage}", e)
                    context?.toastOnUi("登录出错\n${e.localizedMessage}")
                    e.printOnDebug()
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (!oKToClose) {
            execute {
                val loginInfo = viewModel.loginInfo.toMutableMap()
                rowUis?.forEachIndexed { index, rowUi ->
                    when (rowUi.type) {
                        "toggle" -> {
                            loginInfo[rowUi.name] = rowUi.default.toString()
                        }
                    }
                }
                if (loginInfo.isEmpty()) {
                    viewModel.source?.removeLoginInfo()
                } else {
                    viewModel.source?.putLoginInfo(GSON.toJson(loginInfo))
                }
            }
        }
        super.onDismiss(dialog)
        activity?.finish()
    }

}
