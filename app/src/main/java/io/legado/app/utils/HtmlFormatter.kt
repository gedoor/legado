package io.legado.app.utils

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
            .replace("^[\\n\\s]+".toRegex(), "　　")
            .replace("[\\n\\s]+$".toRegex(), "")
    }

    fun formatKeepImg(html: String?) = format(html, notImgHtmlRegex)

    fun formatKeepImg(html: String?, redirectUrl: URL?): String {
        html ?: return ""
        val keepImgHtml = html.replace(wrapHtmlRegex, "\n")
            .replace(notImgHtmlRegex, "")
            .replace("\\n\\s*$|^\\s*\\n".toRegex(), "")
            .replace("\\n\\s*\\n".toRegex(), "\n")
        //正则的“|”处于顶端而不处于（）中时，具有类似||的熔断效果，故以此机制简化原来的代码
        val matcher = Pattern.compile("<img[^>]*src *= *\"([^\"{]*\\{(?:[^{}]|\\{[^}]+\\})+\\})\"[^>]*>|<img[^>]*data-[^=]*= *\"([^\"]*)\"[^>]*>|<img[^>]*src *= *\"([^\"]*)\"[^>]*>", Pattern.CASE_INSENSITIVE).matcher(keepImgHtml)
        var appendPos = 0
        val sb = StringBuffer()
        while (matcher.find()){
            var param = ""
            sb.append( keepImgHtml.substring(appendPos, matcher.start()),"<img src=\"${
                (NetworkUtils.getAbsoluteURL(redirectUrl,
                    matcher.group(1)?.let {
                        val urlMatcher = AnalyzeUrl.paramPattern.matcher(it)
                        if(urlMatcher.find()) {
                            param = ',' + it.substring(urlMatcher.end())
                            it.substring(0,urlMatcher.start())
                        } else it
                    }?: matcher.group(2) ?: matcher.group(3)!!)) + param
            }\">")
            appendPos = matcher.end()
        }
        if (appendPos < keepImgHtml.length) sb.append(keepImgHtml.substring( appendPos, keepImgHtml.length ))
        return sb.toString()
    }
}
