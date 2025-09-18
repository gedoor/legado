package io.legado.app.ui.code

import android.app.Application
import android.content.Intent
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.legado.app.base.BaseViewModel
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.toastOnUi
import org.eclipse.tm4e.core.registry.IThemeSource
import androidx.core.graphics.toColorInt

class CodeEditViewModel(application: Application) : BaseViewModel(application) {
    companion object {
        val themeFileNames = arrayOf("d_monokai_dimmed", "d_monokai", "d_modern", "l_modern", "d_solarized", "l_solarized", "d_abyss", "l_quiet")
        private var durTheme =  ""
        private val themes = mutableMapOf<String, Boolean>()
        private var grammarsLoaded = false
    }
    var initialText = ""
    var language: TextMateLanguage? = null
    var color: TextMateColorScheme? = null
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
        color = TextMateColorScheme.create(themeRegistry)
    }

    fun loadTextMateGrammars() {
        if (!grammarsLoaded) {
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
            grammarsLoaded = true
        }
        language = TextMateLanguage.create("source.js", true)
    }

    fun customColors(editor: CodeEditor) {
        val colorscheme = editor.colorScheme
        if (durTheme.startsWith("d_")) {
            colorscheme.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND, "#60FFFFFF".toColorInt()) //选中括号
            colorscheme.setColor(EditorColorScheme.SCROLL_BAR_THUMB, "#FF27292A".toColorInt())
            colorscheme.setColor(EditorColorScheme.SCROLL_BAR_THUMB_PRESSED, "#90D8D8D8".toColorInt()) //滚动条反色
            colorscheme.setColor(EditorColorScheme.LINE_NUMBER_PANEL_TEXT, "#80D8D8D8".toColorInt()) //选个更喜欢的滚动条提示文本色
        }
        else {
            colorscheme.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND, "#60000000".toColorInt())
        }
    }

}