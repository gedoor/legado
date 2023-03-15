package io.legado.app.utils

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

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

    /**
     * 根据网络url获取文件名
     */
    fun getFileName(fileUrl: String): String? {
        return kotlin.runCatching {
            var fileName = ""
            val url = URL(fileUrl)
            val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
            // head方式
            conn.requestMethod = "HEAD"
            conn.connect()

            // 方法一
            val raw: String? = conn.getHeaderField("Content-Disposition")
            if (raw != null && raw.indexOf("=") > 0) {
                fileName = raw.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                fileName =
                    String(
                        fileName.toByteArray(StandardCharsets.ISO_8859_1),
                        StandardCharsets.UTF_8 //?
                    )
            } else {
                // 方法二 截取
                var newUrl: String = conn.url.path ?: return null
                newUrl = URLDecoder.decode(newUrl, "UTF-8")
                fileName = newUrl.substringAfterLast("/")
            }
            fileName
        }.getOrNull()
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