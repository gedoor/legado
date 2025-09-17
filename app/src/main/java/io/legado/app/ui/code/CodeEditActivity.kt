package io.legado.app.ui.code

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
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
    KeyboardToolPop.CallBack {
    override val binding by viewBinding(ActivityCodeEditBinding::inflate)
    override val viewModel by viewModels<CodeEditViewModel>()
    private val softKeyboardTool by lazy {
        KeyboardToolPop(this, lifecycleScope, binding.root, this)
    }
    var editor: CodeEditor? = null
    var initialText = ""

    private fun initTextMate() {
        viewModel.loadTextMateThemes()
        viewModel.loadTextMateGrammars()
    }

    private fun initView() {
        binding.root.setOnApplyWindowInsetsListenerCompat { _, windowInsets ->
            softKeyboardTool.initialPadding = windowInsets.imeHeight
            windowInsets
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        softKeyboardTool.attachToWindow(window)
        initView()
        initTextMate()
        val language = TextMateLanguage.create("source.js", true)
        initialText = intent.getStringExtra("text") ?: ""
        editor = binding.editText.apply {
            setText(initialText)
            colorScheme = TextMateColorScheme.create(viewModel.themeRegistry)
            setEditorLanguage(language)
            isWordwrap = true
        }
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
        val text = editor!!.text.toString()
        if (text == initialText) {
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

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.code_edit_activity, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> save(false)
            R.id.menu_change_theme -> showDialogFragment(ChangeThemeDialog())
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