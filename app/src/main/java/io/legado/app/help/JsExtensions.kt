package io.legado.app.help

import android.util.Base64
import androidx.annotation.Keep
import io.legado.app.constant.AppConst.dateFormat
import io.legado.app.help.http.CookieStore
import io.legado.app.help.http.SSLHelper
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.*

@Keep
@Suppress("unused")
interface JsExtensions {

    /**
     * js实现跨域访问,不能删
     */
    fun ajax(urlStr: String): String? {
        return try {
            val analyzeUrl = AnalyzeUrl(urlStr)
            val call = analyzeUrl.getResponse(urlStr)
            val response = call.execute()
            response.body()
        } catch (e: Exception) {
            e.msg
        }
    }

    fun connect(urlStr: String): Any {
        return try {
            val analyzeUrl = AnalyzeUrl(urlStr)
            val call = analyzeUrl.getResponse(urlStr)
            val response = call.execute()
            response
        } catch (e: Exception) {
            e.msg
        }
    }

    /**
     * js实现文件下载
     */
    fun downloadFile(content: String, url: String): String {
        val type = AnalyzeUrl(url).type ?: return "type为空，未下载"
        val zipPath = FileUtils.getPath(
            FileUtils.createFolderIfNotExist(FileUtils.getCachePath()),
            "${MD5Utils.md5Encode16(url)}.${type}"
        )
        FileUtils.deleteFile(zipPath)
        val zipFile = FileUtils.createFileIfNotExist(zipPath)
        StringUtils.hexStringToByte(content).let {
            if (it != null) {
                zipFile.writeBytes(it)
            }
        }
        return zipPath
    }

    /**
     * js实现压缩文件解压
     */
    fun unzipFile(zipPath: String): String {
        val unzipPath = FileUtils.getPath(
            FileUtils.createFolderIfNotExist(FileUtils.getCachePath()),
            FileUtils.getNameExcludeExtension(zipPath)
        )
        FileUtils.deleteFile(unzipPath)
        val zipFile = FileUtils.createFileIfNotExist(zipPath)
        val unzipFolder = FileUtils.createFolderIfNotExist(unzipPath)
        ZipUtils.unzipFile(zipFile, unzipFolder)
        FileUtils.deleteFile(zipPath)
        return unzipPath
    }

    /**
     * js实现文件夹内所有文件读取
     */
    fun getTxtInFolder(unzipPath: String): String {
        val unzipFolder = FileUtils.createFolderIfNotExist(unzipPath)
        val contents = StringBuilder()
        unzipFolder.listFiles().let {
            if (it != null) {
                for (f in it) {
                    val charsetName = EncodingDetect.getEncode(f)
                    contents.append(String(f.readBytes(), Charset.forName(charsetName)))
                        .append("\n")
                }
                contents.deleteCharAt(contents.length - 1)
            }
        }
        FileUtils.deleteFile(unzipPath)
        return contents.toString()
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
      *js实现读取cookie
      */
     fun getCookie(tag: String, key: String? = null): String {
        val cookie = CookieStore.getCookie(tag)
        val cookieMap = CookieStore.cookieToMap(cookie)
        return if (key != null) {
            cookieMap[key] ?: ""
        } else {
            cookie
        }
     }

    /**
     * js实现解码,不能删
     */
    fun base64Decode(str: String): String {
        return EncoderUtils.base64Decode(str, Base64.NO_WRAP)
    }

    fun base64Decode(str: String, flags: Int): String {
        return EncoderUtils.base64Decode(str, flags)
    }

    fun base64Encode(str: String): String? {
        return EncoderUtils.base64Encode(str, Base64.NO_WRAP)
    }

    fun base64Encode(str: String, flags: Int): String? {
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

    fun encodeURI(str: String, enc: String): String {
        return try {
            URLEncoder.encode(str, enc)
        } catch (e: Exception) {
            ""
        }
    }

    fun htmlFormat(str: String): String {
        return str.htmlFormat()
    }
}
