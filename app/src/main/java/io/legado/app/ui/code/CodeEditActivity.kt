package io.legado.app.ui.code

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.viewModels
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.widget.CodeEditor
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityCodeEditBinding
import io.legado.app.help.config.ThemeConfig
import io.legado.app.utils.viewbindingdelegate.viewBinding
import org.eclipse.tm4e.core.registry.IThemeSource
import kotlin.getValue

class CodeEditActivity :
    VMBaseActivity<ActivityCodeEditBinding, CodeEditViewModel>() {
    override val binding by viewBinding(ActivityCodeEditBinding::inflate)
    override val viewModel by viewModels<CodeEditViewModel>()
    var editor: CodeEditor? = null
    companion object {
        const val lightName = "monokai_light"
        const val darkName = "monokai_dark"
    }
    private fun initTextMate() {
        val isDarkMode = ThemeConfig.isDarkTheme()
        val themeName = if (isDarkMode) darkName else lightName
        val themeAssetsPath = "textmate/$themeName.json"
        ThemeRegistry.getInstance().apply{
            loadTheme(
                ThemeModel(
                    IThemeSource.fromInputStream(
                        FileProviderRegistry.getInstance().tryGetInputStream(themeAssetsPath),
                        themeAssetsPath,
                        null
                    ), themeName
                ),true
            )
            setTheme(themeName)
        }
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initTextMate()
        val language = TextMateLanguage.create("source.js", true)
        val initialText = intent.getStringExtra("text") ?: ""
        editor = binding.editText
        editor?.apply {
            typefaceText = Typeface.MONOSPACE
            setText(initialText)
            colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            setEditorLanguage(language)
            isWordwrap = true
        }
        binding.btnSave.setOnClickListener {
            val result = Intent().apply {
                putExtra("text", editor?.text.toString())
            }
            setResult(RESULT_OK, result)
            finish()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        editor?.release()
    }
}