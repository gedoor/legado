package io.legado.app.utils

import retrofit2.Response
import java.net.URL
import java.util.*

object NetworkUtils {
    fun getUrl(response: Response<*>): String {
        val networkResponse = response.raw().networkResponse
        return networkResponse?.request?.url?.toString()
            ?: response.raw().request.url.toString()
    }

    private val notNeedEncoding: BitSet by lazy {
        val bitSet = BitSet(256)
        for (i in 'a'.toInt()..'z'.toInt()) {
            bitSet.set(i)
        }
        for (i in 'A'.toInt()..'Z'.toInt()) {
            bitSet.set(i)
        }
        for (i in '0'.toInt()..'9'.toInt()) {
            bitSet.set(i)
        }
        for (char in "+-_.$:()!*@&#,[]") {
            bitSet.set(char.toInt())
        }
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
        }

        return !needEncode
    }

    /**
     * 判断c是否是16进制的字符
     */
    private fun isDigit16Char(c: Char): Boolean {
        return c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f'
    }

    /**
     * 获取绝对地址
     */
    fun getAbsoluteURL(baseURL: String?, relativePath: String?): String? {
        if (baseURL.isNullOrEmpty()) return relativePath
        if (relativePath.isNullOrEmpty()) return baseURL
        var relativeUrl = relativePath
        try {
            val absoluteUrl = URL(baseURL.substringBefore(","))
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