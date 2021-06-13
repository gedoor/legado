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
        val matcher = AppPattern.imgPattern.matcher(formatHtml)
        while (matcher.find()) {
            val urlArray = matcher.group(1)!!.split(AnalyzeUrl.splitUrlRegex)
            var url = NetworkUtils.getAbsoluteURL(redirectUrl, urlArray[0])
            if (urlArray.size > 1) {
                url = "$url,${urlArray[1]}"
            }
            //将Matcher上次匹配结尾到本次匹配结尾这段字符串序列追加到sb中，且是先将其中匹配到的部分替换后再追加
            matcher.appendReplacement(sb, "<img src=\"$url\" >")
        }
        //将Matcher最后那个匹配之后的字串匹配到追加到sb中
        matcher.appendTail(sb)

        return sb.toString()
    }

}
