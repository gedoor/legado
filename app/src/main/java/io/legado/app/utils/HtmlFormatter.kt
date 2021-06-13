package io.legado.app.utils

import io.legado.app.model.analyzeRule.AnalyzeUrl
import java.net.URL
import io.legado.app.constant.AppPattern

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

    fun formatKeepImg(html: String?) = format(html,notImgHtmlRegex)

    fun formatKeepImg(html: String?, redirectUrl: URL?): String {
        html ?: return ""
        var formatHtml = formatKeepImg(html)
        val sb = StringBuffer()
        var appendPos = 0
        while (appendPos < formatHtml.length) {
            val matcher = AppPattern.imgPattern.matcher(formatHtml)
            if(matcher.find()) {
                val urlArray = matcher.group(1)!!.split(AnalyzeUrl.splitUrlRegex)
                var url = NetworkUtils.getAbsoluteURL(redirectUrl, urlArray[0])
                if (urlArray.size > 1) {
                    url = "$url,${urlArray[1]}"
                }
                sb.append(formatHtml.substring(appendPos, matcher.start()))
                sb.append("<img src=\"$url\" >")
                appendPos = matcher.end()
                formatHtml = formatHtml.substring(appendPos, formatHtml.length)
                appendPos = 0
            } else {
                sb.append(formatHtml)
                appendPos = formatHtml.length
            }
        }

        return sb.toString()
    }

}
