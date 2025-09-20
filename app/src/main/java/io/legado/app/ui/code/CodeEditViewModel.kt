package io.legado.app.ui.code

import android.app.Application
import android.content.Intent
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.legado.app.base.BaseViewModel
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.toastOnUi
import org.eclipse.tm4e.core.registry.IThemeSource

class CodeEditViewModel(application: Application) : BaseViewModel(application) {
    companion object {
        val themeFileNames = arrayOf("d_monokai_dimmed", "d_monokai", "d_modern", "l_modern", "d_solarized", "l_solarized", "d_abyss", "l_quiet")
        private var grammarsLoaded = false
    }
    var initialText = ""
    var language: TextMateLanguage? = null
    val themeRegistry: ThemeRegistry = ThemeRegistry.getInstance()

    fun initData(
        intent: Intent,
        success: () -> Unit
    ) {
        execute {
            initialText = intent.getStringExtra("text") ?: throw NoStackTraceException("未获取到待编辑文本")
        }.onSuccess {
            success.invoke()
        }.onError {
            context.toastOnUi("error\n${it.localizedMessage}")
            it.printOnDebug()
        }
        loadTextMateThemes()
        loadTextMateGrammars()
    }

    fun loadTextMateThemes(index: Int? = null) {
        val theme = themeFileNames.getOrElse(index ?: AppConfig.editTheme) { "d_monokai" }
        val themeModel = themeRegistry.findThemeByFileName(theme)
        if (themeModel == null) {
            val themeAssetsPath = "textmate/$theme.json"
            val themeSource = IThemeSource.fromInputStream(FileProviderRegistry.getInstance().tryGetInputStream(themeAssetsPath), themeAssetsPath, null)
            themeRegistry.loadTheme(ThemeModel(themeSource, theme).apply {
                isDark = theme.startsWith("d_")
            })
        }
        else {
            themeRegistry.setTheme(themeModel)
        }
    }

    fun loadTextMateGrammars() {
        if (!grammarsLoaded) {
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
            grammarsLoaded = true
        }
        language = TextMateLanguage.create("source.js", true)
    }

}