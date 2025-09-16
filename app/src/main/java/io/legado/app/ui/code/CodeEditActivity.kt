package io.legado.app.ui.code

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.widget.CodeEditor
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityCodeEditBinding
import io.legado.app.help.config.ThemeConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.viewbindingdelegate.viewBinding
import org.eclipse.tm4e.core.registry.IThemeSource
import kotlin.getValue

class CodeEditActivity :
    VMBaseActivity<ActivityCodeEditBinding, CodeEditViewModel>() {
    override val binding by viewBinding(ActivityCodeEditBinding::inflate)
    override val viewModel by viewModels<CodeEditViewModel>()
    var editor: CodeEditor? = null
    var initialText = ""

    companion object {
        const val lightName = "monokai_light"
        const val darkName = "monokai_dark"
        private var durTheme =  ""
        val themes = mutableMapOf(lightName to false, darkName to false)
        private var grammarsLoaded = false
        private fun loadTextMateGrammars() {
            if (!grammarsLoaded) {
                GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
                grammarsLoaded = true
            }
        }
        private fun loadTextMateThemes(theme: String) {
            if (durTheme != theme) {
                if (themes[theme] != true) {
                    val themeAssetsPath = "textmate/$theme.json"
                    val themeSource = IThemeSource.fromInputStream(FileProviderRegistry.getInstance().tryGetInputStream(themeAssetsPath), themeAssetsPath, null)
                    val  themeModel= ThemeModel(themeSource, theme)
                    themeModel.isDark = true
                    ThemeRegistry.getInstance().loadTheme(themeModel, false)
                    themes[theme] = true
                }
                ThemeRegistry.getInstance().setTheme(theme)
                durTheme = theme
            }
        }
    }

    private fun initTextMate() {
        val isDarkMode = ThemeConfig.isDarkTheme()
        val themeName = if (isDarkMode) darkName else lightName
        loadTextMateThemes(themeName)
        loadTextMateGrammars()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initTextMate()
        val language = TextMateLanguage.create("source.js", true)
        val color = TextMateColorScheme.create(ThemeRegistry.getInstance())
        initialText = intent.getStringExtra("text") ?: ""
        editor = binding.editText.apply {
            setText(initialText)
            colorScheme = color
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
            R.id.menu_log -> showDialogFragment<AppLogDialog>()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun finish() {
        save(true)
    }
}