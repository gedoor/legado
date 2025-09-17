package io.legado.app.ui.code

import android.app.Application
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.legado.app.base.BaseViewModel
import io.legado.app.help.config.AppConfig
import org.eclipse.tm4e.core.registry.IThemeSource

class CodeEditViewModel(application: Application) : BaseViewModel(application) {
    companion object {
        val themeFileNames = arrayOf("d_monokai_dimmed", "d_monokai", "d_modern", "l_modern", "d_solarized", "l_solarized")
        private var durTheme =  ""
        private val themes = mutableMapOf<String, Boolean>()
        private var grammarsLoaded = false
    }
    val themeRegistry: ThemeRegistry = ThemeRegistry.getInstance()
    fun loadTextMateThemes() {
        val theme = themeFileNames[AppConfig.editTheme]
        if (durTheme != theme) {
            if (themes[theme] != true) {
                val themeAssetsPath = "textmate/$theme.json"
                val themeSource = IThemeSource.fromInputStream(FileProviderRegistry.getInstance().tryGetInputStream(themeAssetsPath), themeAssetsPath, null)
                val  themeModel= ThemeModel(themeSource, theme)
                themeModel.isDark = theme.startsWith("d_")
                themeRegistry.loadTheme(themeModel, false)
                themes[theme] = true
            }
            themeRegistry.setTheme(theme)
            durTheme = theme
        }
    }
    fun loadTextMateGrammars() {
        if (!grammarsLoaded) {
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
            grammarsLoaded = true
        }
    }

}