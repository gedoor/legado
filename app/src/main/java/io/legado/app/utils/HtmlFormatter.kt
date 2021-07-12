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
            .replace("^[\\n\\s]*".toRegex(), "　　")
            .replace("[\\n\\s]+$".toRegex(), "")
    }

    fun formatKeepImg(html: String?) = format(html, notImgHtmlRegex)

    fun formatKeepImg(html: String?, redirectUrl: URL?): String {
        html ?: return ""
        val keepImgHtml = formatKeepImg(html)
        val sb = StringBuffer()

        //图片有data-开头的数据属性时优先用数据属性作为src，没有数据属性时匹配src
        val imgPattern = Pattern.compile(
            if(keepImgHtml.matches("　　<img[^>]*data-".toRegex())) "<img[^>]*data-[^=]*= *\"([^\"])\"[^>]*>"
            else "　　<img[^>]*src *= *\"([^\"{]+(?:\\{(?:[^{}]|\\{[^{}]*\\})*\\})?)\"[^>]*>", Pattern.CASE_INSENSITIVE
        )

        val matcher = imgPattern.matcher(keepImgHtml)
        var appendPos = 0

        if(matcher.find()){
            var url = matcher.group(1)!!
            var pos = url.indexOf(',')
            sb.append(keepImgHtml.substring(appendPos, matcher.start()))
            sb.append(
                "<img src=\"${
                    if (pos == -1) url else NetworkUtils.getAbsoluteURL(
                        redirectUrl,
                        url.substring(0, pos)
                    ) + url.substring(pos)
                }\">"
            )
            appendPos = matcher.end()
            if(pos == -1) {
                while (matcher.find()) {
                    sb.append(keepImgHtml.substring(appendPos, matcher.start()))
                    sb.append( "<img src=\"${
                        NetworkUtils.getAbsoluteURL(redirectUrl,matcher.group(1)!!)
                    }\">" )
                    appendPos = matcher.end()
                }
            }else{
                while (matcher.find()) {
                    url = matcher.group(1)!!
                    pos = url.indexOf(',')
                    sb.append(keepImgHtml.substring(appendPos, matcher.start()))
                    sb.append(
                        "<img src=\"${
                            NetworkUtils.getAbsoluteURL(
                                redirectUrl,
                                url.substring(0, pos)
                            )
                        }${
                            url.substring(pos)
                        }\">"
                    )
                    appendPos = matcher.end()
                }
            }
        }

        if (appendPos < keepImgHtml.length) {
            sb.append(keepImgHtml.substring(appendPos, keepImgHtml.length))
        }
        return sb.toString()
    }

}
