package io.legado.app.ui.replace.edit

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.databinding.ActivityReplaceEditBinding
import io.legado.app.lib.dialogs.SelectItem
import io.legado.app.ui.code.CodeEditActivity
import io.legado.app.ui.widget.keyboard.KeyboardToolPop
import io.legado.app.utils.GSON
import io.legado.app.utils.imeHeight
import io.legado.app.utils.sendToClip
import io.legado.app.utils.setOnApplyWindowInsetsListenerCompat
import io.legado.app.utils.showHelp
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * 编辑替换规则
 */
class ReplaceEditActivity :
    VMBaseActivity<ActivityReplaceEditBinding, ReplaceEditViewModel>(),
    KeyboardToolPop.CallBack {

    companion object {

        fun startIntent(
            context: Context,
            id: Long = -1,
            pattern: String? = null,
            isRegex: Boolean = false,
            scope: String? = null
        ): Intent {
            val intent = Intent(context, ReplaceEditActivity::class.java)
            intent.putExtra("id", id)
            intent.putExtra("pattern", pattern)
            intent.putExtra("isRegex", isRegex)
            intent.putExtra("scope", scope)
            return intent
        }

    }

    override val binding by viewBinding(ActivityReplaceEditBinding::inflate)
    override val viewModel by viewModels<ReplaceEditViewModel>()

    private val softKeyboardTool by lazy {
        KeyboardToolPop(this, lifecycleScope, binding.root, this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        softKeyboardTool.attachToWindow(window)
        initView()
        viewModel.initData(intent) {
            upReplaceView(it)
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.replace_edit, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    private val textEditLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val editedText = result.data?.getStringExtra("text")
            editedText?.let {
                val view = window.decorView.findFocus()
                if (view is EditText) {
                    view.setText(it)
                    view.setSelection(result.data!!.getIntExtra("cursorPosition", 0))
                } else {
                    toastOnUi(R.string.focus_lost_on_textbox)
                }
            }
        }
    }
    private fun onFullEditClicked() {
        val view = window.decorView.findFocus()
        if (view is EditText) {
            val currentText = view.text.toString()
            val intent = Intent(this, CodeEditActivity::class.java).apply {
                putExtra("text", currentText)
                putExtra("cursorPosition", view.selectionStart)
            }
            textEditLauncher.launch(intent)
        }
        else {
            toastOnUi(R.string.please_focus_cursor_on_textbox)
        }
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_fullscreen_edit -> onFullEditClicked()
            R.id.menu_save -> viewModel.save(getReplaceRule()) {
                setResult(RESULT_OK)
                finish()
            }

            R.id.menu_copy_rule -> sendToClip(GSON.toJson(getReplaceRule()))
            R.id.menu_paste_rule -> viewModel.pasteRule {
                upReplaceView(it)
            }
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        softKeyboardTool.dismiss()
    }

    private fun initView() {
        binding.ivHelp.setOnClickListener {
            showHelp("regexHelp")
        }
        binding.root.setOnApplyWindowInsetsListenerCompat { _, windowInsets ->
            softKeyboardTool.initialPadding = windowInsets.imeHeight
            windowInsets
        }
    }

    private fun upReplaceView(replaceRule: ReplaceRule) = binding.run {
        etName.setText(replaceRule.name)
        etGroup.setText(replaceRule.group)
        etReplaceRule.setText(replaceRule.pattern)
        cbUseRegex.isChecked = replaceRule.isRegex
        etReplaceTo.setText(replaceRule.replacement)
        cbScopeTitle.isChecked = replaceRule.scopeTitle
        cbScopeContent.isChecked = replaceRule.scopeContent
        etScope.setText(replaceRule.scope)
        etExcludeScope.setText(replaceRule.excludeScope)
        etTimeout.setText(replaceRule.timeoutMillisecond.toString())
    }

    private fun getReplaceRule(): ReplaceRule = binding.run {
        val replaceRule: ReplaceRule = viewModel.replaceRule ?: ReplaceRule()
        replaceRule.name = etName.text.toString()
        replaceRule.group = etGroup.text.toString()
        replaceRule.pattern = etReplaceRule.text.toString()
        replaceRule.isRegex = cbUseRegex.isChecked
        replaceRule.replacement = etReplaceTo.text.toString()
        replaceRule.scopeTitle = cbScopeTitle.isChecked
        replaceRule.scopeContent = cbScopeContent.isChecked
        replaceRule.scope = etScope.text.toString()
        replaceRule.excludeScope = etExcludeScope.text.toString()
        replaceRule.timeoutMillisecond = etTimeout.text.toString().ifEmpty { "3000" }.toLong()
        return replaceRule
    }

    override fun helpActions(): List<SelectItem<String>> {
        return arrayListOf(
            SelectItem("正则教程", "regexHelp")
        )
    }

    override fun onHelpActionSelect(action: String) {
        when (action) {
            "regexHelp" -> showHelp("regexHelp")
        }
    }

    override fun sendText(text: String) {
        if (text.isEmpty()) return
        val view = window?.decorView?.findFocus()
        if (view is EditText) {
            val start = view.selectionStart
            val end = view.selectionEnd
            //获取EditText的文字
            val edit = view.editableText
            if (start < 0 || start >= edit.length) {
                edit.append(text)
            } else {
                //光标所在位置插入文字
                edit.replace(start, end, text)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onUndoClicked() {
        val editText = window.decorView.findFocus()
        if (editText is EditText) {
            editText.onTextContextMenuItem(android.R.id.undo)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRedoClicked() {
        val editText = window.decorView.findFocus()
        if (editText is EditText) {
            editText.onTextContextMenuItem(android.R.id.redo)
        }
    }

}
