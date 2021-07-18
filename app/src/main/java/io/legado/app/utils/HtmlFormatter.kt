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

        var str = StringBuffer()
        var endPos = 0
        var hasMatch = true //普通图片标签是否还未处理过
        var hasMatchX = false //是否存在带参数或带数据属性的图片标签
        var pos = -1
        val list = listOf("<img[^>]*src *= *\"([^\"{]*\\{(?:[^{}]|\\{[^}]+\\})+\\})\"[^>]*>","<img[^>]*data-[^=]*= *\"([^\"]*)\"[^>]*>","<img[^>]*src *= *\"([^\"]*)\"[^>]*>") //优先匹配用户处理过所以带参数的图片标签，其次匹配带数据属性的图片标签
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
                        val matcher0 = Pattern.compile("<img[^>]*src *= *\"([^\"]*)\"[^>]*>", Pattern.CASE_INSENSITIVE).matcher(strBefore) //格式化普通图片标签
                        while (matcher0.find()) {
                            sb.append(strBefore.substring(appendPos0, matcher0.start()),"<img src=\"${NetworkUtils.getAbsoluteURL(redirectUrl,matcher0.group(1)!!)}\">")
                            appendPos0 = matcher0.end()
                        }
                        strBefore = strBefore.substring(appendPos0, strBefore.length)
                    }

                    sb.append( strBefore,"<img src=\"${
                        if (pos == 0) {
                            val url = matcher.group(1)!!
                            val urlMatcher = AnalyzeUrl.paramPattern.matcher(url)
                            NetworkUtils.getAbsoluteURL(redirectUrl,url.substring(0,urlMatcher.start())) + ',' + url.substring(urlMatcher.end())
                        }else NetworkUtils.getAbsoluteURL(redirectUrl,matcher.group(1)!!)
                    }\">")

                    appendPos = matcher.end()
                } while (matcher.find())
                hasMatch = false //普通图片标签已经处理过
            }

            if (appendPos != 0) {
                hasMatchX = true //存在带参数或带数据属性的图片标签
                endPos = appendPos //存在匹配，更新位置
                str = sb //存在匹配，更新字符串
            }
        }

        if (endPos < keepImgHtml.length) {
            str.append(
                    if(hasMatchX){ //处理末尾的普通图片标签
                        var appendPos0 = 0
                        val strBefore = keepImgHtml.substring(endPos, keepImgHtml.length)
                        val matcher0 =
                            Pattern.compile("<img[^>]*src *= *\"([^\"]*)\"[^>]*>", Pattern.CASE_INSENSITIVE)
                                .matcher(strBefore) //格式化普通图片标签
                        while (matcher0.find()) {
                            str.append(strBefore.substring(appendPos0, matcher0.start()),"<img src=\"${ NetworkUtils.getAbsoluteURL( redirectUrl, matcher0.group(1)!! )  }\">")
                            appendPos0 = matcher0.end()
                        }
                        strBefore.substring( appendPos0, strBefore.length )
                    }else keepImgHtml.substring( endPos, keepImgHtml.length )
                    )  //缩进图片之后的非空白段落
        }
        return str.toString()
    }
}
