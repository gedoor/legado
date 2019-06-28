package io.legado.app.utils

import android.text.TextUtils
import java.net.URL
import java.util.*

object NetworkUtils {

    private val notNeedEncoding: BitSet by lazy {
        val bitSet = BitSet(256)
        var i: Int = 'a'.toInt()
        while (i <= 'z'.toInt()) {
            bitSet.set(i)
            i++
        }
        i = 'A'.toInt()
        while (i <= 'Z'.toInt()) {
            bitSet.set(i)
            i++
        }
        i = '0'.toInt()
        while (i <= '9'.toInt()) {
            bitSet.set(i)
            i++
        }
        bitSet.set('+'.toInt())
        bitSet.set('-'.toInt())
        bitSet.set('_'.toInt())
        bitSet.set('.'.toInt())
        bitSet.set('$'.toInt())
        bitSet.set(':'.toInt())
        bitSet.set('('.toInt())
        bitSet.set(')'.toInt())
        bitSet.set('!'.toInt())
        bitSet.set('*'.toInt())
        bitSet.set('@'.toInt())
        bitSet.set('&'.toInt())
        bitSet.set('#'.toInt())
        bitSet.set(','.toInt())
        bitSet.set('['.toInt())
        bitSet.set(']'.toInt())
        return@lazy bitSet
    }

    /**
     * 支持JAVA的URLEncoder.encode出来的string做判断。 即: 将' '转成'+'
     * 0-9a-zA-Z保留 <br></br>
     * ! * ' ( ) ; : @ & = + $ , / ? # [ ] 保留
     * 其他字符转成%XX的格式，X是16进制的大写字符，范围是[0-9A-F]
     */
    fun hasUrlEncoded(str: String): Boolean {
        var needEncode = false
        var i = 0
        while (i < str.length) {
            val c = str[i]
            if (notNeedEncoding.get(c.toInt())) {
                i++
                continue
            }
            if (c == '%' && i + 2 < str.length) {
                // 判断是否符合urlEncode规范
                val c1 = str[++i]
                val c2 = str[++i]
                if (isDigit16Char(c1) && isDigit16Char(c2)) {
                    i++
                    continue
                }
            }
            // 其他字符，肯定需要urlEncode
            needEncode = true
            break
            i++
        }

        return !needEncode
    }

    /**
     * 判断c是否是16进制的字符
     */
    private fun isDigit16Char(c: Char): Boolean {
        return c >= '0' && c <= '9' || c >= 'A' && c <= 'F'
    }

    /**
     * 获取绝对地址
     */
    fun getAbsoluteURL(baseURL: String, relativePath: String): String {
        var relativeUrl = relativePath
        if (TextUtils.isEmpty(baseURL)) return relativePath
        try {
            val absoluteUrl = URL(baseURL)
            val parseUrl = URL(absoluteUrl, relativePath)
            relativeUrl = parseUrl.toString()
            return relativeUrl
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return relativeUrl
    }

    fun getBaseUrl(url: String?): String? {
        if (url == null || !url.startsWith("http")) return null
        val index = url.indexOf("/", 9)
        return if (index == -1) {
            url
        } else url.substring(0, index)
    }

}