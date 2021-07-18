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
            .replace("[\\n\\s]+\$|^[\\n\\s]+".toRegex(), "")
            .replace("\\s*\\n+\\s*".toRegex(), "\n")

        var str = ""
        var endPos = 0
        var hasMatch = true //普通图片标签是否还未处理过
        var hasMatchX = false //是否存在带参数或带数据属性的图片标签
        var pos = -1
        val list = listOf("<img[^>]*src *= *\"([^\"{]+\\{(?:[^{}]|\\{[^}]+})+})\"[^>]*>","<img[^>]*data-[^=]*= *\"([^\"]+)\"[^>]*>","<img[^>]*src *= *\"([^\"]+)\"[^>]*>") //优先匹配用户处理过所以带参数的图片标签，其次匹配带数据属性的图片标签
        while(++pos<3){
            if(pos == 2) {
                if(hasMatchX)break //普通图片标签只在不存在存在带参数或带数据属性的图片标签的时候匹配
                else hasMatch = false //匹配带参数或带数据属性的图片标签同时也会格式化普通图片标签
            }
            var appendPos = 0
            val matcher =
                Pattern.compile(list[pos], Pattern.CASE_INSENSITIVE).matcher(keepImgHtml)
            val sb = StringBuffer()
            if (matcher.find()) {
                do {
                    var strBefore = keepImgHtml.substring(appendPos, matcher.start())

                    if (hasMatch) { //格式化不带参数和数据属性的普通图片标签
                        var appendPos0 = 0
                        val matcher0 = Pattern.compile("<img[^>]*src *= *\"([^\"]+)\"[^>]*>", Pattern.CASE_INSENSITIVE).matcher(strBefore) //格式化普通图片标签
                        while (matcher0.find()) {
                            val strBefore0 = strBefore.substring(appendPos, matcher.start())
                            sb.append( if (strBefore0.isBlank()) strBefore0 else strBefore0.replace("\n","\n　　"))
                            sb.append("<img src=\"${NetworkUtils.getAbsoluteURL(redirectUrl,matcher.group(1)!!)}\">")
                            appendPos0 = matcher.end()
                        }
                        strBefore = if (appendPos0 < strBefore.length) {
                            strBefore.substring(appendPos0, strBefore.length).replace("\n", "\n　　") //缩进图片之后的非空白段落
                        } else ""
                    }

                    sb.append(
                        if (strBefore.isBlank()) strBefore else strBefore.replace("\n","\n　　") //缩进图片之间的非空白段落
                    )

                    if (pos == 0) {
                        val url = matcher.group(1)!!
                        val urlMatcher = AnalyzeUrl.paramPattern.matcher(url)
                        val find = urlMatcher.find()
                        sb.append("<img src=\"${
                            if(find) NetworkUtils.getAbsoluteURL(redirectUrl,url.substring(0,urlMatcher.start())) + url.substring(urlMatcher.end())
                            else url
                        }\">")

                    } else sb.append("<img src=\"${NetworkUtils.getAbsoluteURL(redirectUrl,matcher.group(1)!!)}\">")

                    appendPos = matcher.end()
                } while (matcher.find())
                hasMatch = false //普通图片标签已经处理过
            }

            if (appendPos != 0) {
                hasMatchX = true //存在带参数或带数据属性的图片标签
                endPos = appendPos //存在匹配，更新位置
                str = sb.toString() //存在匹配，更新字符串
            }
        }

        return if (endPos < keepImgHtml.length) {
            str + keepImgHtml.substring( endPos, keepImgHtml.length ).replace("\n","\n　　") //缩进图片之后的非空白段落
        }else str
    }
}
