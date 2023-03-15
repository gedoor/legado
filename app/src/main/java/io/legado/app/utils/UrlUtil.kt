package io.legado.app.utils

import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.constant.AppLog
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


    /* 阅读定义的url,{urlOption} */
    fun getFileName(analyzeUrl: AnalyzeUrl): String? {
        return getFileName(analyzeUrl.url, analyzeUrl.headerMap)
    }

    /**
     * 根据网络url获取文件信息 文件名
     */
    fun getFileName(fileUrl: String, headerMap: Map<String, String>? = null): String? {
        // 如果获取到后缀可直接截取链接
        if (getSuffix(fileUrl, "") != "") return fileUrl.substringAfterLast("/")
        return kotlin.runCatching {
            var fileName: String = ""
            val url = URL(fileUrl)
            val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
            // HEAD方式获取返回头信息
            conn.requestMethod = "HEAD"
            // 下载链接可能还需要书源header才能成功访问
            headerMap?.forEach { key, value ->
                conn.setRequestProperty(key, value)
            }
            conn.connect()

            // val contentLength = conn.getContentLengthLong()
            // Content-Disposition
            // 解析文件名 filename= filename*=
            val raw: String? = conn.getHeaderField("Content-Disposition")
            if (raw != null && raw.indexOf("=") > 0) {
                fileName = raw.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                fileName =
                    String(
                        fileName.toByteArray(StandardCharsets.ISO_8859_1),
                        StandardCharsets.UTF_8 //?
                    )
            } else if (conn.getHeaderField("Location") != null) {
                // Location跳转到实际的下载链接
                var newUrl: String = conn.url.path ?: return null
                newUrl = URLDecoder.decode(newUrl, "UTF-8")
                if (getSuffix(newUrl, "") != "") {
                    fileName = newUrl.substringAfterLast("/")
                }
            } else {
                // 其余情况 返回响应头
                val headers = conn.getHeaderFields()
                val headersString = buildString {
                    headers.forEach { key, value ->
                        value.forEach {
                            append(key)
                            append(": ")
                            append(it)
                            append("\n")
                        }
                    }
                }
                AppLog.putDebug("Cannot obtain URL file name:\n$headersString")
            }
            fileName
        }.getOrNull()
    }

    /* 获取合法的文件后缀 */
    fun getSuffix(url: String, default: String? = null): String {
        val suffix = url.substringAfterLast(".").substringBeforeLast(",")
        //检查截取的后缀字符是否合法 [a-zA-Z0-9]
        val fileSuffixRegex = Regex("^[a-z\\d]+$", RegexOption.IGNORE_CASE)
        return if (suffix.length > 5 || !suffix.matches(fileSuffixRegex)) {
            default ?: throw IllegalArgumentException("Cannot find illegal suffix")
        } else {
            suffix
        }
    }

}