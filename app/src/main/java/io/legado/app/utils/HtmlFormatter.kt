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
            .replace("[\\n\\s]+\$|^[\\n\\s]*".toRegex(), "")
            .replace("\\s*\\n+\\s*".toRegex(), "\n")

        val sb = StringBuffer()
        val hasDataType:Boolean //是否有数据属性

        //图片有data-开头的数据属性时优先用数据属性作为src，没有数据属性时匹配src
        val imgPattern = Pattern.compile(
            if(keepImgHtml.matches("<img[^>]*data-".toRegex())) {
                hasDataType = true
                "<img[^>]*data-[^=]*= *\"([^\"])\"[^>]*>"
            }
            else {
                hasDataType = false
                "<img[^>]*src *= *\"([^\"{]+(?:\\{(?:[^{}]|\\{[^{}]*\\})*\\})?)\"[^>]*>"
            }, Pattern.CASE_INSENSITIVE
        )

        val matcher = imgPattern.matcher(keepImgHtml)
        var appendPos = 0

        if(matcher.find()){
            if(hasDataType || matcher.group(1)!!.indexOf(',') == -1) { //图片无参

                do{
                    sb.append(keepImgHtml.substring(appendPos, matcher.start()).replace("\n","\n　　")) //非图片部分换行缩进
                    sb.append( "<img src=\"${
                        NetworkUtils.getAbsoluteURL(redirectUrl,matcher.group(1)!!)
                    }\">" )
                    appendPos = matcher.end()
                }while (matcher.find())

            }else{ //图片有参

                do{
                    val url = matcher.group(1)!!
                    val urlBefore = url.substringBefore(',')
                    val beforeStr = keepImgHtml.substring(appendPos, matcher.start())
                    sb.append( if(beforeStr == "\n") '\n' else beforeStr.replace("\n","\n　　") ) //缩进换行下个非图片段落
                    sb.append(
                        "<img src=\"${
                            NetworkUtils.getAbsoluteURL(
                                redirectUrl,
                                urlBefore
                            )
                        }${
                            url.substring(urlBefore.length)
                        }\">"
                    )
                    appendPos = matcher.end()
                }while(matcher.find())

            }
        }

        if (appendPos < keepImgHtml.length) {
            val beforeStr = keepImgHtml.substring(appendPos, keepImgHtml.length)
            sb.append( if(beforeStr == "\n") '\n' else beforeStr.replace("\n","\n　　") ) //缩进换行下个非图片段落
        }
        return sb.toString()
    }

}
