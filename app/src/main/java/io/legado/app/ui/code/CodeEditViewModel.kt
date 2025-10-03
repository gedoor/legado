package io.legado.app.ui.code

import android.app.Application
import android.content.Intent
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
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
import org.jsoup.Jsoup

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
            initialText =
                intent.getStringExtra("text") ?: throw NoStackTraceException("未获取到待编辑文本")
            if (isHtmlStr(initialText)) {
                languageName = "text.html.basic"
            } else {
                intent.getStringExtra("languageName")?.let { languageName = it }
            }
            language = TextMateLanguage.create(languageName, AppConfig.editAutoComplete)
            cursorPosition = intent.getIntExtra("cursorPosition", 0)
            writable = intent.getBooleanExtra("writable", true)
        }.onSuccess {
            success.invoke()
        }.onError {
            context.toastOnUi("error\n${it.localizedMessage}")
            it.printOnDebug()
        }
        loadTextMateThemes()
    }

    private fun isHtmlStr(text: String): Boolean {
        val trimmedText = text.trim()
        val htmlRegex = Regex("""^(?:\[[\s\d.]])?<(?:html|!DOCTYPE)""", RegexOption.IGNORE_CASE)
        return htmlRegex.containsMatchIn(trimmedText) && trimmedText.endsWith(">")
    }

    fun loadTextMateThemes(index: Int? = null) {
        val theme = themeFileNames.getOrElse(index ?: AppConfig.editTheme) { "d_monokai" }
        val themeModel = themeRegistry.findThemeByFileName(theme)
        if (themeModel == null) {
            val themeAssetsPath = "textmate/$theme.json"
            val themeSource = IThemeSource.fromInputStream(
                FileProviderRegistry.getInstance().tryGetInputStream(themeAssetsPath),
                themeAssetsPath,
                null
            )
            themeRegistry.loadTheme(ThemeModel(themeSource, theme).apply {
                isDark = theme.startsWith("d_")
            })
        } else {
            themeRegistry.setTheme(themeModel)
        }
    }

    fun formatCode(editor: CodeEditor) {
        execute {
            val text = editor.text.toString()
            if (languageName.contains("markdown")) {
                context.toastOnUi("markdown不需要格式化")
                return@execute text
            }
            val isHtml = languageName.contains("html")
            if (isHtml) {
                return@execute webFormatCodeHtml(text)
            }
            val beautifyJs = FileProviderRegistry.getInstance()
                .tryGetInputStream("beautify.min.js")
                ?.use { inputStream -> inputStream.bufferedReader().readText() } ?: ""
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
                    result += webFormatCode(beautifyJs, jsCode)
                } else if (jsMatcher.group(1) != null) {
                    result += "<js>\n"
                    val jsCode = jsMatcher.group(1)!!
                    result += webFormatCode(beautifyJs, jsCode)
                    result += "\n</js>"
                }
                start = jsMatcher.end()
            }
            if (start == 0) {
                val jsCode = text
                result += webFormatCode(beautifyJs, jsCode)
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

    private suspend fun webFormatCode(beautifyJs: String, jsCode: String): String? {
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
        } catch (e: Exception) {
            context.toastOnUi("格式化失败")
            jsCode
        }
    }

    private fun webFormatCodeHtml(html: String): String? {
        return try {
            val doc = Jsoup.parse(html)
            doc.outputSettings()
                .indentAmount(4)
                .prettyPrint(true)
            doc.outerHtml()
        } catch (e: Exception) {
            context.toastOnUi("格式化失败")
            html
        }
    }

}