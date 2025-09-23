package io.legado.app.ui.code

import android.app.Application
import android.content.Intent
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.widget.CodeEditor
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppPattern.JS_PATTERN
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.config.AppConfig
import io.legado.app.help.http.BackstageWebView
import io.legado.app.utils.escapeForJs
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.toastOnUi
import org.eclipse.tm4e.core.registry.IThemeSource

class CodeEditViewModel(application: Application) : BaseViewModel(application) {
    companion object {
        val themeFileNames = arrayOf(
            "d_monokai_dimmed",
            "d_monokai",
            "d_modern",
            "l_modern",
            "d_solarized",
            "l_solarized",
            "d_abyss",
            "l_quiet"
        )
        private var grammarsLoaded = false
    }

    var initialText = ""
    var cursorPosition = 0
    var language: TextMateLanguage? = null
    var languageName = "source.js"
    val themeRegistry: ThemeRegistry = ThemeRegistry.getInstance()
    var writable = true

    fun initData(
        intent: Intent, success: () -> Unit
    ) {
        execute {
            initialText = intent.getStringExtra("text") ?: throw NoStackTraceException("未获取到待编辑文本")
            cursorPosition = intent.getIntExtra("cursorPosition", 0)
            writable = intent.getBooleanExtra("writable", true)
        }.onSuccess {
            success.invoke()
        }.onError {
            context.toastOnUi("error\n${it.localizedMessage}")
            it.printOnDebug()
        }
        loadTextMateThemes()
        intent.getStringExtra("languageName")?.let{ languageName = it }
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
        language = TextMateLanguage.create(languageName, true)
    }

    fun formatCode(editor: CodeEditor) {
        execute {
            val text = editor.text.toString()
            val isHtml = languageName.contains("html")
            val beautifyJs = FileProviderRegistry.getInstance().tryGetInputStream(if(isHtml) "beautify-html.min.js" else "beautify.min.js")
                ?.use { inputStream -> inputStream.bufferedReader().readText() } ?: ""
            if (isHtml) {
                return@execute webFormatCodeHtml(beautifyJs,text)
            }
            var start = 0
            val jsMatcher = JS_PATTERN.matcher(text)
            var result = ""
            while (jsMatcher.find()) {
                if (jsMatcher.start() > start) {
                    result += text.substring(start, jsMatcher.start()).trim()
                }
                if (jsMatcher.group(2) != null) {
                    result += "@js:\n"
                    val jsCode = jsMatcher.group(2)!!
                    result += webFormatCode(beautifyJs,jsCode)
                } else if (jsMatcher.group(1) != null) {
                    result += "<js>\n"
                    val jsCode = jsMatcher.group(1)!!
                    result += webFormatCode(beautifyJs,jsCode)
                    result += "\n</js>"
                }
                start = jsMatcher.end()
            }
            if (start == 0) {
                val jsCode = text
                result += webFormatCode(beautifyJs,jsCode)
                start = text.length
            }
            if (text.length > start) {
                result += text.substring(start).trim()
            }
            result
        }.onSuccess {
            editor.setText(it)
        }.onError {
            context.toastOnUi("格式化失败")
        }
    }

    private suspend fun webFormatCode (beautifyJs: String, jsCode: String): String? {
        return try {
            BackstageWebView(
                url = null,
                html = "<body>",
                javaScript = """$beautifyJs
                js_beautify("${jsCode.escapeForJs()}", {
                indent_size: 4,
                indent_char: ' ',
                preserve_newlines: true,
                max_preserve_newlines: 5,
                brace_style: 'collapse',
                space_before_conditional: true,
                unescape_strings: false,
                jslint_happy: false,
                end_with_newline: false,
                wrap_line_length: 0,
                comma_first: false
                });""".trimIndent(),
                headerMap = null,
                tag = null
            ).getStrResponse().body
        } catch(e: Exception){
            context.toastOnUi("格式化失败")
            jsCode
        }
    }

    private suspend fun webFormatCodeHtml (beautifyJs: String, html: String): String? {
        return try {
            BackstageWebView(
                url = null,
                html = "<body>",
                javaScript = """$beautifyJs
                html_beautify("${html.escapeForJs()}", {
                indent_size: 4,
                indent_char: ' ',
                indent_with_tabs: false,
                preserve_newlines: true,
                max_preserve_newlines: 5,
                wrap_line_length: 80,
                wrap_attributes: 'auto',
                wrap_attributes_indent_size: 4,
                unformatted: ['code', 'pre'],
                indent_inner_html: true,
                indent_scripts: 'keep',
                extra_liners: []
                });""".trimIndent(),
                headerMap = null,
                tag = null
            ).getStrResponse().body
        } catch(e: Exception){
            context.toastOnUi("格式化失败")
            html
        }
    }

}