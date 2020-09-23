package io.legado.app.help

import android.util.Base64
import androidx.annotation.Keep
import io.legado.app.constant.AppConst.dateFormat
import io.legado.app.help.http.SSLHelper
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.*

@Keep
@Suppress("unused")
interface JsExtensions {

    /**
     * js实现跨域访问,不能删
     */
    fun ajax(urlStr: String): String? {
        return try {
            val analyzeUrl = AnalyzeUrl(urlStr, null, null, null, null, null)
            val call = analyzeUrl.getResponse(urlStr)
            val response = call.execute()
            response.body()
        } catch (e: Exception) {
            e.msg
        }
    }

    fun connect(urlStr: String): Any {
        return try {
            val analyzeUrl = AnalyzeUrl(urlStr, null, null, null, null, null)
            val call = analyzeUrl.getResponse(urlStr)
            val response = call.execute()
            response
        } catch (e: Exception) {
            e.msg
        }
    }

    /**
     * js实现重定向拦截,不能删
     */
    fun get(urlStr: String, headers: Map<String, String>): Connection.Response {
        return Jsoup.connect(urlStr)
            .sslSocketFactory(SSLHelper.unsafeSSLSocketFactory)
            .ignoreContentType(true)
            .followRedirects(false)
            .headers(headers)
            .method(Connection.Method.GET)
            .execute()
    }

    fun post(urlStr: String, body: String, headers: Map<String, String>): Connection.Response {
        return Jsoup.connect(urlStr)
            .sslSocketFactory(SSLHelper.unsafeSSLSocketFactory)
            .ignoreContentType(true)
            .followRedirects(false)
            .requestBody(body)
            .headers(headers)
            .method(Connection.Method.POST)
            .execute()
    }

    /**
     * js实现解码,不能删
     */
    fun base64Decode(str: String): String {
        return EncoderUtils.base64Decode(str)
    }

    fun base64Encode(str: String): String? {
        return EncoderUtils.base64Encode(str)
    }

    fun base64Encode(str: String, flags: Int = Base64.NO_WRAP): String? {
        return EncoderUtils.base64Encode(str, flags)
    }

    fun md5Encode(str: String): String {
        return MD5Utils.md5Encode(str)
    }

    fun md5Encode16(str: String): String {
        return MD5Utils.md5Encode16(str)
    }

    fun timeFormat(time: Long): String {
        return dateFormat.format(Date(time))
    }

    //utf8编码转gbk编码
    fun utf8ToGbk(str: String): String {
        val utf8 = String(str.toByteArray(charset("UTF-8")))
        val unicode = String(utf8.toByteArray(), charset("UTF-8"))
        return String(unicode.toByteArray(charset("GBK")))
    }

    fun encodeURI(str: String): String {
        return try {
            URLEncoder.encode(str, "UTF-8")
        } catch (e: Exception) {
            ""
        }
    }

    fun htmlFormat(str: String): String {
        return str.htmlFormat()
    }
}
