package io.legado.app.utils

import android.text.TextUtils
import io.legado.app.utils.icu4j.CharsetDetector
import org.jsoup.Jsoup
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * <Detect encoding .> Copyright (C) <2009> <Fluck></Fluck>,ACC http://androidos.cc/dev>
 *
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 *
 * EncodingDetect.java<br></br>
 * 自动获取文件的编码
 *
 * @author Billows.Van
 * @version 1.0
 * @since Create on 2010-01-27 11:19:00
</Detect> */
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
        val byteArray: ByteArray = ByteArray(2000)
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