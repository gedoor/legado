package io.legado.app.utils

import io.legado.app.constant.AppPattern.imgPattern
import io.legado.app.model.analyzeRule.AnalyzeUrl
import java.net.URL
import java.util.regex.Pattern

object HtmlFormatter {
    private val wrapHtmlRegex = "</?(?:div|p|br|hr|h\\d|article|dd|dl)[^>]*>".toRegex()
    private val notImgHtmlRegex = "</?(?!img)[a-zA-Z]+(?=[ >])[^<>]*>".toRegex()
    private val otherHtmlRegex = "</?[a-zA-Z]+(?=[ >])[^<>]*>".toRegex()

    fun format(html: String?, otherRegex: Regex = otherHtmlRegex): String {
        html ?: return ""
        return html.replace(wrapHtmlRegex, "\n")
            .replace(otherRegex, "")
            .replace("\\s*\\n+\\s*".toRegex(), "\n　　")
            .replace("^[\\n\\s]*".toRegex(), "　　")
            .replace("[\\n\\s]+$".toRegex(), "")
    }

    fun formatKeepImg(html: String?) = format(html, notImgHtmlRegex)

    fun formatKeepImg(html: String?, redirectUrl: URL?): String {
        html ?: return ""
        val keepImgHtml = formatKeepImg(html)
        val sb = StringBuffer()

        //图片有data-开头的数据属性时优先用数据属性作为src，没有数据属性时才匹配src
        val hasData = keepImgHtml.matches("<img[^>]*data-".toRegex())

        val imgPatternX = if(hasData) Pattern.compile("<img[^>]*data-[^=]*= *\"([^\"])\"[^>]*>", Pattern.CASE_INSENSITIVE) else imgPattern

        val matcher = imgPatternX.matcher(keepImgHtml)
        var appendPos = 0
        while (matcher.find()) {

            var url = matcher.group(1)!!
            val param:String

            url = NetworkUtils.getAbsoluteURL(redirectUrl, if(url.indexOf(',') != -1) {
                val absoluteUrl = url.split(AnalyzeUrl.splitUrlRegex, 1)[0]
                param = url.substring(absoluteUrl.length)
                absoluteUrl
            } else {
                param = ""
                url
            })

            sb.append(keepImgHtml.substring(appendPos, matcher.start()))
            sb.append("<img src=\"${url+param}\" >")
            appendPos = matcher.end()

        }
        if (appendPos < keepImgHtml.length) {
            sb.append(keepImgHtml.substring(appendPos, keepImgHtml.length))
        }
        return sb.toString()
    }

}
