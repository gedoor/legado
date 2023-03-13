package io.legado.app.utils

object UrlUtil {

    fun replaceReservedChar(text: String): String {
        return text.replace("%", "%25")
            .replace(" ", "%20")
            .replace("\"", "%22")
            .replace("#", "%23")
            .replace("&", "%26")
            .replace("(", "%28")
            .replace(")", "%29")
            .replace("+", "%2B")
            .replace(",", "%2C")
            .replace("/", "%2F")
            .replace(":", "%3A")
            .replace(";", "%3B")
            .replace("<", "%3C")
            .replace("=", "%3D")
            .replace(">", "%3E")
            .replace("?", "%3F")
            .replace("@", "%40")
            .replace("\\", "%5C")
            .replace("|", "%7C")
    }

    fun getSuffix(url: String, default: String): String {
        val suffix = url.substringAfterLast(".").substringBeforeLast(",")
        //检查截取的后缀字符是否合法 [a-zA-Z0-9]
        val fileSuffixRegex = Regex("^[a-z0-9]+$", RegexOption.IGNORE_CASE)
        return if (suffix.length > 5 || !suffix.matches(fileSuffixRegex)) {
            default
        } else {
            suffix
        }
    }

}