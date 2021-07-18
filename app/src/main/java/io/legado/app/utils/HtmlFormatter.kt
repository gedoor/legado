package io.legado.app.utils

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
            .replace("[\\n\\s]+\$|^[\\n\\s]+".toRegex(), "")
            .replace("\\s*\\n+\\s*".toRegex(), "\n")

        val sb = StringBuffer()
        var endPos = 0
        for(pattern in listOf("<img[^>]*src *= *\"([^\"{]+(?:\\{(?:[^{}]|\\{[^{}]*\\})*\\})?)\"[^>]*>","<img[^>]*data-[^=]*= *\"([^\"])\"[^>]*>")){
            var appendPos = 0
            val matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(keepImgHtml)
            while (matcher.find()) {
                val url = matcher.group(1)!!
                val urlBefore = url.substringBefore(',')
                val strBefore = keepImgHtml.substring(appendPos, matcher.start())
                sb.append(
                    if(strBefore.isBlank()) strBefore else strBefore.replace("\n", "\n　　") //缩进图片之间的非空白段落
                )
                sb.append(
                    "<img src=\"${
                        NetworkUtils.getAbsoluteURL(
                            redirectUrl,
                            urlBefore
                        ) + url.substring(urlBefore.length)
                    }\">"
                )
                appendPos = matcher.end()
            }
            if(appendPos != 0)endPos = appendPos
        }

        if (endPos < keepImgHtml.length) {
            sb.append( keepImgHtml.substring( endPos, keepImgHtml.length ).replace("\n","\n　　") ) //缩进图片之后的非空白段落
        }
        return sb.toString()
    }

}
