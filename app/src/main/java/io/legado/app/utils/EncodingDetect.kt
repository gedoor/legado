package io.legado.app.utils

import android.text.TextUtils
import io.legado.app.lib.icu4j.CharsetDetector
import org.jsoup.Jsoup
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * 自动获取文件的编码
 * */
@Suppress("MemberVisibilityCanBePrivate", "unused")
object EncodingDetect {

    fun getHtmlEncode(bytes: ByteArray): String? {
        try {
            val doc = Jsoup.parse(String(bytes, StandardCharsets.UTF_8))
            val metaTags = doc.getElementsByTag("meta")
            var charsetStr: String
            for (metaTag in metaTags) {
                charsetStr = metaTag.attr("charset")
                if (!TextUtils.isEmpty(charsetStr)) {
                    return charsetStr
                }
                val content = metaTag.attr("content")
                val httpEquiv = metaTag.attr("http-equiv")
                if (httpEquiv.toLowerCase(Locale.getDefault()) == "content-type") {
                    charsetStr = if (content.toLowerCase(Locale.getDefault()).contains("charset")) {
                        content.substring(
                            content.toLowerCase(Locale.getDefault())
                                .indexOf("charset") + "charset=".length
                        )
                    } else {
                        content.substring(content.toLowerCase(Locale.getDefault()).indexOf(";") + 1)
                    }
                    if (!TextUtils.isEmpty(charsetStr)) {
                        return charsetStr
                    }
                }
            }
        } catch (ignored: Exception) {
        }
        return getEncode(bytes)
    }

    fun getEncode(bytes: ByteArray): String {
        val detector = CharsetDetector()
        detector.setText(bytes)
        val match = detector.detect()
        return match.name
    }

    /**
     * 得到文件的编码
     */
    fun getEncode(filePath: String): String {
        return getEncode(File(filePath))
    }

    /**
     * 得到文件的编码
     */
    fun getEncode(file: File): String {
        val tempByte = getFileBytes(file)
        return getEncode(tempByte)
    }

    private fun getFileBytes(testFile: File?): ByteArray {
        val fis: FileInputStream
        val byteArray = ByteArray(2000)
        try {
            fis = FileInputStream(testFile)
            fis.read(byteArray)
            fis.close()
        } catch (e: Exception) {
            System.err.println("Error: $e")
        }
        return byteArray
    }
}