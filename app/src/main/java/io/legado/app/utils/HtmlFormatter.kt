package io.legado.app.utils

import java.net.URL
import java.util.regex.Pattern

object HtmlFormatter {
    private val wrapHtmlRegex = "</?(?:div|p|br|hr|h\\d|article|dd|dl)[^>]*>".toRegex()
    private val notImgHtmlRegex = "</?(?!img)[a-zA-Z]+(?=[ >])[^<>]*>".toRegex()
    private val otherHtmlRegex = "</?[a-zA-Z]+(?=[ >])[^<>]*>".toRegex()
    private val imgPattern = Pattern.compile("<img [^>]*src=.*?\"(.*?(?:,\\{.*\\})?)\".*?>")

    fun format(html: String?): String {
        html ?: return ""
        return html.replace(wrapHtmlRegex, "\n")
            .replace(otherHtmlRegex, "")
            .replace("\\s*\\n+\\s*".toRegex(), "\n　　")
            .replace("^[\\n\\s]+".toRegex(), "　　")
            .replace("[\\n\\s]+$".toRegex(), "")
    }

    fun formatKeepImg(html: String?): String {
        html ?: return ""
        return html.replace(wrapHtmlRegex, "\n")
            .replace(notImgHtmlRegex, "")
            .replace("\\s*\\n+\\s*".toRegex(), "\n　　")
            .replace("^[\\n\\s]+".toRegex(), "　　")
            .replace("[\\n\\s]+$".toRegex(), "")
    }

    fun formatKeepImg(html: String?, redirectUrl: URL?): String {
        html ?: return ""
        val sb = StringBuffer()
        val matcher = imgPattern.matcher(html)
        while (matcher.find()) {
            val url = NetworkUtils.getAbsoluteURL(redirectUrl, matcher.group(1)!!)
            matcher.appendReplacement(sb, "<img src=\"$url\" >")
        }
        matcher.appendTail(sb)
        return sb.replace(wrapHtmlRegex, "\n")
            .replace(notImgHtmlRegex, "")
            .replace("\\s*\\n+\\s*".toRegex(), "\n　　")
            .replace("^[\\n\\s]+".toRegex(), "　　")
            .replace("[\\n\\s]+$".toRegex(), "")
    }

}