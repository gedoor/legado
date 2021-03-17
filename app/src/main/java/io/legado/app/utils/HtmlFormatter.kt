package io.legado.app.utils

import io.legado.app.model.analyzeRule.AnalyzeUrl
import java.net.URL
import java.util.regex.Pattern

object HtmlFormatter {
    private val wrapHtmlRegex = "</?(?:div|p|br|hr|h\\d|article|dd|dl)[^>]*>".toRegex()
    private val notImgHtmlRegex = "</?(?!img)[a-zA-Z]+(?=[ >])[^<>]*>".toRegex()
    private val otherHtmlRegex = "</?[a-zA-Z]+(?=[ >])[^<>]*>".toRegex()
    private val imgPattern = Pattern.compile("<img [^>]*src=.*?\"(.*?(?:,\\{.*\\})?)\".*?>")

    fun format(html: String?): String {
        html ?: return ""
        return html.replace(wrapHtmlRegex, "\n")
            .replace(otherHtmlRegex, "")
            .replace("\\s*\\n+\\s*".toRegex(), "\n　　")
            .replace("^[\\n\\s]+".toRegex(), "　　")
            .replace("[\\n\\s]+$".toRegex(), "")
    }

    fun formatKeepImg(html: String?): String {
        html ?: return ""
        return html.replace(wrapHtmlRegex, "\n")
            .replace(notImgHtmlRegex, "")
            .replace("\\s*\\n+\\s*".toRegex(), "\n　　")
            .replace("^[\\n\\s]+".toRegex(), "　　")
            .replace("[\\n\\s]+$".toRegex(), "")
    }

    fun formatKeepImg(html: String?, redirectUrl: URL?): String {
        html ?: return ""
        val sb = StringBuffer()
        val matcher = imgPattern.matcher(html)
        var appendPos = 0
        while (matcher.find()) {
            val urlArray = matcher.group(1)!!.split(AnalyzeUrl.splitUrlRegex)
            var url = NetworkUtils.getAbsoluteURL(redirectUrl, urlArray[0])
            if (urlArray.size > 1) {
                url = "$url,${urlArray[1]}"
            }
            sb.append(html.substring(appendPos, matcher.start()))
            sb.append("<img src=\"$url\" >")
            appendPos = matcher.end()
        }
        if (appendPos < html.length) {
            sb.append(html.substring(appendPos, html.length))
        }
        return sb.replace(wrapHtmlRegex, "\n")
            .replace(notImgHtmlRegex, "")
            .replace("\\s*\\n+\\s*".toRegex(), "\n　　")
            .replace("^[\\n\\s]+".toRegex(), "　　")
            .replace("[\\n\\s]+$".toRegex(), "")
    }

}