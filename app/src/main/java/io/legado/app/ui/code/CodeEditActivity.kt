package io.legado.app.ui.code

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityCodeEditBinding
import io.legado.app.lib.dialogs.SelectItem
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.code.changetheme.ChangeThemeDialog
import io.legado.app.ui.widget.keyboard.KeyboardToolPop
import io.legado.app.utils.imeHeight
import io.legado.app.utils.setOnApplyWindowInsetsListenerCompat
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.showHelp
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlin.getValue

class CodeEditActivity :
    VMBaseActivity<ActivityCodeEditBinding, CodeEditViewModel>(),
    KeyboardToolPop.CallBack, ChangeThemeDialog.DialogCallback {
    override val binding by viewBinding(ActivityCodeEditBinding::inflate)
    override val viewModel by viewModels<CodeEditViewModel>()
    private val softKeyboardTool by lazy {
        KeyboardToolPop(this, lifecycleScope, binding.root, this)
    }
    var editor: CodeEditor? = null


    private fun initView() {
        binding.root.setOnApplyWindowInsetsListenerCompat { _, windowInsets ->
            softKeyboardTool.initialPadding = windowInsets.imeHeight
            windowInsets
        }
        binding.editText.apply {
            colorScheme = TextMateColorScheme2(ThemeRegistry.getInstance(), ThemeRegistry.getInstance().currentThemeModel)
            isWordwrap = true
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        softKeyboardTool.attachToWindow(window)
        viewModel.initData(intent) {
            editor = binding.editText.apply {
                setText(viewModel.initialText)
                setEditorLanguage(viewModel.language)
            }
        }
        initView()
    }

    override fun onDestroy() {
        super.onDestroy()
        editor?.release()
    }

    /**
    * 使用super.finish(),防止循环回调
    * */
    private fun save(check: Boolean) {
        editor ?: return
        val text = editor?.text.toString()
        if (text == viewModel.initialText) {
            super.finish()
        } else if (check) {
            alert(R.string.exit) {
                setMessage(R.string.exit_no_save)
                positiveButton(R.string.yes)
                negativeButton(R.string.no) {
                    super.finish()
                }
            }
        } else {
            val result = Intent().apply {
                putExtra("text", text)
                putExtra("cursorPosition", editor?.cursor?.left ?: 0)
            }
            setResult(RESULT_OK, result)
            super.finish()
        }
    }

    override fun upTheme(index: Int){
        viewModel.loadTextMateThemes(index)
        editor?.setEditorLanguage(viewModel.language) //每次更改颜色后需要再执行一次语言设置,防止切换主题后高亮颜色不正确
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.code_edit_activity, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> save(false)
            R.id.menu_change_theme -> showDialogFragment(ChangeThemeDialog(this))
            R.id.menu_log -> showDialogFragment<AppLogDialog>()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun finish() {
        save(true)
    }

    override fun helpActions(): List<SelectItem<String>> {
        return arrayListOf(
            SelectItem("源教程", "ruleHelp"),
            SelectItem("js教程", "jsHelp"),
            SelectItem("正则教程", "regexHelp")
        )
    }

    override fun onHelpActionSelect(action: String) {
        when (action) {
            "ruleHelp" -> showHelp("ruleHelp")
            "jsHelp" -> showHelp("jsHelp")
            "regexHelp" -> showHelp("regexHelp")
        }
    }

    override fun sendText(text: String) {
        editor?.insertText(text, text.length)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onUndoClicked() {
        editor?.undo()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRedoClicked() {
        editor?.redo()
    }
}