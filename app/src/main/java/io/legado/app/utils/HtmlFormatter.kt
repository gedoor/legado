package io.legado.app.utils

import io.legado.app.constant.AppPattern
import io.legado.app.model.analyzeRule.AnalyzeUrl
import java.net.URL

object HtmlFormatter {
    private val wrapHtmlRegex = "</?(?:div|p|br|hr|h\\d|article|dd|dl)[^>]*>".toRegex()
    private val notImgHtmlRegex = "</?(?!img)[a-zA-Z]+(?=[ >])[^<>]*>".toRegex()
    private val otherHtmlRegex = "</?[a-zA-Z]+(?=[ >])[^<>]*>".toRegex()

    fun format(html: String?, otherRegex: Regex = otherHtmlRegex): String {
        html ?: return ""
        return html.replace(wrapHtmlRegex, "\n")
            .replace(otherRegex, "")
            .replace("^[\\n\\s]+".toRegex(), "　　")
            .replace("[\\n\\s]+$".toRegex(), "")
            .replace("\\s*\\n+\\s*".toRegex(), "\n　　")
    }

    fun formatKeepImg(html: String?) = format(html, notImgHtmlRegex)

    fun formatKeepImg(html: String?, redirectUrl: URL?): String {
        html ?: return ""
        val keepImgHtml = formatKeepImg(html)
        val sb = StringBuffer()
        val matcher = AppPattern.imgPattern.matcher(keepImgHtml)
        var appendPos = 0
        while (matcher.find()) {
            val urlArray = matcher.group(1)!!.split(AnalyzeUrl.splitUrlRegex)
            var url = NetworkUtils.getAbsoluteURL(redirectUrl, urlArray[0])
            if (urlArray.size > 1) {
                url = "$url,${urlArray[1]}"
            }
            sb.append(keepImgHtml.substring(appendPos, matcher.start()))
            sb.append("<img src=\"$url\" >")
            appendPos = matcher.end()
        }
        if (appendPos < keepImgHtml.length) {
            sb.append(keepImgHtml.substring(appendPos, keepImgHtml.length))
        }
        return sb.toString()
    }

}
