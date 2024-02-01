package io.legado.app.utils

import io.legado.app.model.analyzeRule.AnalyzeUrl
import java.net.URL
import java.util.regex.Pattern

@Suppress("RegExpRedundantEscape")
object HtmlFormatter {
    private val nbspRegex = "(&nbsp;)+".toRegex()
    private val espRegex = "(&ensp;|&emsp;)".toRegex()
    private val noPrintRegex = "(&thinsp;|&zwnj;|&zwj;|\u2009|\u200C|\u200D)".toRegex()
    private val wrapHtmlRegex = "</?(?:div|p|br|hr|h\\d|article|dd|dl)[^>]*>".toRegex()
    private val commentRegex = "<!--[^>]*-->".toRegex() //注释
    private val notImgHtmlRegex = "</?(?!img)[a-zA-Z]+(?=[ >])[^<>]*>".toRegex()
    private val otherHtmlRegex = "</?[a-zA-Z]+(?=[ >])[^<>]*>".toRegex()
    private val formatImagePattern = Pattern.compile(
        "<img[^>]*\\ssrc\\s*=\\s*\"([^\"{>]*\\{(?:[^{}]|\\{[^}>]+\\})+\\})\"[^>]*>|<img[^>]*\\sdata-[^=>]*=\\s*\"([^\">]*)\"[^>]*>|<img[^>]*\\ssrc\\s*=\\s*\"([^\">]*)\"[^>]*>",
        Pattern.CASE_INSENSITIVE
    )
    private val indent1Regex = "\\s*\\n+\\s*".toRegex()
    private val indent2Regex = "^[\\n\\s]+".toRegex()
    private val lastRegex = "[\\n\\s]+$".toRegex()

    fun format(html: String?, otherRegex: Regex = otherHtmlRegex): String {
        html ?: return ""
        return html.replace(nbspRegex, " ")
            .replace(espRegex, " ")
            .replace(noPrintRegex, "")
            .replace(wrapHtmlRegex, "\n")
            .replace(commentRegex, "")
            .replace(otherRegex, "")
            .replace(indent1Regex, "\n　　")
            .replace(indent2Regex, "　　")
            .replace(lastRegex, "")
    }

    fun formatKeepImg(html: String?, redirectUrl: URL? = null): String {
        html ?: return ""
        val keepImgHtml = format(html, notImgHtmlRegex)

        //正则的“|”处于顶端而不处于（）中时，具有类似||的熔断效果，故以此机制简化原来的代码
        val matcher = formatImagePattern.matcher(keepImgHtml)
        var appendPos = 0
        val sb = StringBuilder()
        while (matcher.find()) {
            var param = ""
            sb.append(
                keepImgHtml.substring(appendPos, matcher.start()), "<img src=\"${
                    NetworkUtils.getAbsoluteURL(
                        redirectUrl,
                        matcher.group(1)?.let {
                            val urlMatcher = AnalyzeUrl.paramPattern.matcher(it)
                            if (urlMatcher.find()) {
                                param = ',' + it.substring(urlMatcher.end())
                                it.substring(0, urlMatcher.start())
                            } else it
                        } ?: matcher.group(2) ?: matcher.group(3)!!
                    ) + param
                }\">"
            )
            appendPos = matcher.end()
        }
        if (appendPos < keepImgHtml.length) sb.append(
            keepImgHtml.substring(
                appendPos,
                keepImgHtml.length
            )
        )
        return sb.toString()
    }
}
