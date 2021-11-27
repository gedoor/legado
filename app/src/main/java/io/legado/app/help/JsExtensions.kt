package io.legado.app.help

import android.net.Uri
import android.util.Base64
import androidx.annotation.Keep
import io.legado.app.BuildConfig
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppConst.dateFormat
import io.legado.app.data.entities.BaseSource
import io.legado.app.help.http.*
import io.legado.app.model.Debug
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.analyzeRule.QueryTTF
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jsoup.Connection
import org.jsoup.Jsoup
import splitties.init.appCtx
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLEncoder
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * js扩展类, 在js中通过java变量调用
 * 所有对于文件的读写删操作都是相对路径,只能操作阅读缓存内的文件
 * /android/data/{package}/cache/...
 */
@Keep
@Suppress("unused")
interface JsExtensions {

    fun getSource(): BaseSource?

    /**
     * 访问网络,返回String
     */
    fun ajax(urlStr: String): String? {
        return runBlocking {
            kotlin.runCatching {
                val analyzeUrl = AnalyzeUrl(urlStr, source = getSource())
                analyzeUrl.getStrResponseAwait().body
            }.onFailure {
                log("ajax(${urlStr}) error\n${it.stackTraceToString()}")
                Timber.e(it)
            }.getOrElse {
                it.msg
            }
        }
    }

    /**
     * 并发访问网络
     */
    fun ajaxAll(urlList: Array<String>): Array<StrResponse?> {
        return runBlocking {
            val asyncArray = Array(urlList.size) {
                async(IO) {
                    val url = urlList[it]
                    val analyzeUrl = AnalyzeUrl(url, source = getSource())
                    analyzeUrl.getStrResponseAwait()
                }
            }
            val resArray = Array<StrResponse?>(urlList.size) {
                asyncArray[it].await()
            }
            resArray
        }
    }

    /**
     * 访问网络,返回Response<String>
     */
    fun connect(urlStr: String): StrResponse {
        return runBlocking {
            val analyzeUrl = AnalyzeUrl(urlStr, source = getSource())
            kotlin.runCatching {
                analyzeUrl.getStrResponseAwait()
            }.onFailure {
                log("connect(${urlStr}) error\n${it.stackTraceToString()}")
                Timber.e(it)
            }.getOrElse {
                StrResponse(analyzeUrl.url, it.localizedMessage)
            }
        }
    }

    fun connect(urlStr: String, header: String?): StrResponse {
        return runBlocking {
            val headerMap = GSON.fromJsonObject<Map<String, String>>(header)
            val analyzeUrl = AnalyzeUrl(urlStr, headerMapF = headerMap, source = getSource())
            kotlin.runCatching {
                analyzeUrl.getStrResponseAwait()
            }.onFailure {
                log("ajax($urlStr,$header) error\n${it.stackTraceToString()}")
                Timber.e(it)
            }.getOrElse {
                StrResponse(analyzeUrl.url, it.localizedMessage)
            }
        }
    }

    /**
     * 使用webView访问网络
     * @param html 直接用webView载入的html, 如果html为空直接访问url
     * @param url html内如果有相对路径的资源不传入url访问不了
     * @param js 用来取返回值的js语句, 没有就返回整个源代码
     * @return 返回js获取的内容
     */
    fun webView(html: String?, url: String?, js: String?): String? {
        return runBlocking {
            BackstageWebView(
                url = url,
                html = html,
                javaScript = js
            ).getStrResponse().body
        }
    }

    /**
     * 实现16进制字符串转文件
     * @param content 需要转成文件的16进制字符串
     * @param url 通过url里的参数来判断文件类型
     * @return 相对路径
     */
    fun downloadFile(content: String, url: String): String {
        val type = AnalyzeUrl(url, source = getSource()).type ?: return ""
        val zipPath = FileUtils.getPath(
            FileUtils.createFolderIfNotExist(FileUtils.getCachePath()),
            "${MD5Utils.md5Encode16(url)}.${type}"
        )
        FileUtils.deleteFile(zipPath)
        val zipFile = FileUtils.createFileIfNotExist(zipPath)
        StringUtils.hexStringToByte(content).let {
            if (it.isNotEmpty()) {
                zipFile.writeBytes(it)
            }
        }
        return zipPath.substring(FileUtils.getCachePath().length)
    }

    /**
     * js实现重定向拦截,网络访问get
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

    /**
     * 网络访问post
     */
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

    fun base64DecodeToByteArray(str: String?): ByteArray? {
        if (str.isNullOrBlank()) {
            return null
        }
        return Base64.decode(str, Base64.DEFAULT)
    }

    fun base64DecodeToByteArray(str: String?, flags: Int): ByteArray? {
        if (str.isNullOrBlank()) {
            return null
        }
        return Base64.decode(str, flags)
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

    /**
     * 格式化时间
     */
    fun timeFormatUTC(time: Long, format: String, sh: Int): String? {
        val utc = SimpleTimeZone(sh, "UTC")
        return SimpleDateFormat(format, Locale.getDefault()).run {
            timeZone = utc
            format(Date(time))
        }
    }

    /**
     * 时间格式化
     */
    fun timeFormat(time: Long): String {
        return dateFormat.format(Date(time))
    }

    /**
     * utf8编码转gbk编码
     */
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
        return HtmlFormatter.formatKeepImg(str)
    }

    //****************文件操作******************//

    /**
     * 获取本地文件
     * @param path 相对路径
     * @return File
     */
    fun getFile(path: String): File {
        val cachePath = appCtx.externalCache.absolutePath
        val aPath = if (path.startsWith(File.separator)) {
            cachePath + path
        } else {
            cachePath + File.separator + path
        }
        return File(aPath)
    }

    fun readFile(path: String): ByteArray? {
        val file = getFile(path)
        if (file.exists()) {
            return file.readBytes()
        }
        return null
    }

    fun readTxtFile(path: String): String {
        val file = getFile(path)
        if (file.exists()) {
            val charsetName = EncodingDetect.getEncode(file)
            return String(file.readBytes(), charset(charsetName))
        }
        return ""
    }

    fun readTxtFile(path: String, charsetName: String): String {
        val file = getFile(path)
        if (file.exists()) {
            return String(file.readBytes(), charset(charsetName))
        }
        return ""
    }

    /**
     * 删除本地文件
     */
    fun deleteFile(path: String) {
        val file = getFile(path)
        FileUtils.delete(file, true)
    }

    /**
     * js实现压缩文件解压
     * @param zipPath 相对路径
     * @return 相对路径
     */
    fun unzipFile(zipPath: String): String {
        if (zipPath.isEmpty()) return ""
        val unzipPath = FileUtils.getPath(
            FileUtils.createFolderIfNotExist(FileUtils.getCachePath()),
            FileUtils.getNameExcludeExtension(zipPath)
        )
        FileUtils.deleteFile(unzipPath)
        val zipFile = getFile(zipPath)
        val unzipFolder = FileUtils.createFolderIfNotExist(unzipPath)
        ZipUtils.unzipFile(zipFile, unzipFolder)
        FileUtils.deleteFile(zipFile.absolutePath)
        return unzipPath.substring(FileUtils.getCachePath().length)
    }

    /**
     * js实现文件夹内所有文件读取
     */
    fun getTxtInFolder(unzipPath: String): String {
        if (unzipPath.isEmpty()) return ""
        val unzipFolder = getFile(unzipPath)
        val contents = StringBuilder()
        unzipFolder.listFiles().let {
            if (it != null) {
                for (f in it) {
                    val charsetName = EncodingDetect.getEncode(f)
                    contents.append(String(f.readBytes(), charset(charsetName)))
                        .append("\n")
                }
                contents.deleteCharAt(contents.length - 1)
            }
        }
        FileUtils.deleteFile(unzipFolder.absolutePath)
        return contents.toString()
    }

    /**
     * 获取网络zip文件里面的数据
     * @param url zip文件的链接或十六进制字符串
     * @param path 所需获取文件在zip内的路径
     * @return zip指定文件的数据
     */
    fun getZipStringContent(url: String, path: String): String {
        val byteArray = getZipByteArrayContent(url, path) ?: return ""
        val charsetName = EncodingDetect.getEncode(byteArray)
        return String(byteArray, Charset.forName(charsetName))
    }

    fun getZipStringContent(url: String, path: String, charsetName: String): String {
        val byteArray = getZipByteArrayContent(url, path) ?: return ""
        return String(byteArray, Charset.forName(charsetName))
    }

    /**
     * 获取网络zip文件里面的数据
     * @param url zip文件的链接或十六进制字符串
     * @param path 所需获取文件在zip内的路径
     * @return zip指定文件的数据
     */
    fun getZipByteArrayContent(url: String, path: String): ByteArray? {
        val bytes = if (url.startsWith("http://") || url.startsWith("https://")) {
            runBlocking {
                return@runBlocking okHttpClient.newCallResponseBody { url(url) }.bytes()
            }
        } else {
            StringUtils.hexStringToByte(url)
        }
        val bos = ByteArrayOutputStream()
        val zis = ZipInputStream(ByteArrayInputStream(bytes))
        var entry: ZipEntry? = zis.nextEntry
        while (entry != null) {
            if (entry.name.equals(path)) {
                zis.use { it.copyTo(bos) }
                return bos.toByteArray()
            }
            entry = zis.nextEntry
        }
        log("getZipContent 未发现内容")
        return null
    }

    //******************文件操作************************//

    /**
     * 解析字体,返回字体解析类
     */
    fun queryBase64TTF(base64: String?): QueryTTF? {
        base64DecodeToByteArray(base64)?.let {
            return QueryTTF(it)
        }
        return null
    }

    /**
     * 返回字体解析类
     * @param str 支持url,本地文件,base64,自动判断,自动缓存
     */
    fun queryTTF(str: String?): QueryTTF? {
        str ?: return null
        val key = md5Encode16(str)
        var qTTF = CacheManager.getQueryTTF(key)
        if (qTTF != null) return qTTF
        val font: ByteArray? = when {
            str.isAbsUrl() -> runBlocking {
                var x = CacheManager.getByteArray(key)
                if (x == null) {
                    x = okHttpClient.newCallResponseBody { url(str) }.bytes()
                    x.let {
                        CacheManager.put(key, it)
                    }
                }
                return@runBlocking x
            }
            str.isContentScheme() -> Uri.parse(str).readBytes(appCtx)
            str.startsWith("/storage") -> File(str).readBytes()
            else -> base64DecodeToByteArray(str)
        }
        font ?: return null
        qTTF = QueryTTF(font)
        CacheManager.put(key, qTTF)
        return qTTF
    }

    /**
     * @param text 包含错误字体的内容
     * @param font1 错误的字体
     * @param font2 正确的字体
     */
    fun replaceFont(
        text: String,
        font1: QueryTTF?,
        font2: QueryTTF?
    ): String {
        if (font1 == null || font2 == null) return text
        val contentArray = text.toCharArray()
        contentArray.forEachIndexed { index, s ->
            val oldCode = s.code
            if (font1.inLimit(s)) {
                val glyf = font1.getGlyfByCode(oldCode)
                val code = font2.getCodeByGlyf(glyf)
                if (code != 0) {
                    contentArray[index] = code.toChar()
                }
            }
        }
        return contentArray.joinToString("")
    }

    /**
     * 输出调试日志
     */
    fun log(msg: String): String {
        getSource()?.let {
            Debug.log(it.getKey(), msg)
        } ?: Debug.log(msg)
        if (BuildConfig.DEBUG) {
            Timber.d(msg)
        }
        return msg
    }

    /**
     * 生成UUID
     */
    fun randomUUID(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * AES 解码为 ByteArray
     * @param str 传入的AES加密的数据
     * @param key AES 解密的key
     * @param transformation AES加密的方式
     * @param iv ECB模式的偏移向量
     */
    fun aesDecodeToByteArray(
        str: String, key: String, transformation: String, iv: String
    ): ByteArray? {
        return try {
            EncoderUtils.decryptAES(
                data = str.encodeToByteArray(),
                key = key.encodeToByteArray(),
                transformation,
                iv.encodeToByteArray()
            )
        } catch (e: Exception) {
            Timber.e(e)
            log(e.localizedMessage ?: "aesDecodeToByteArrayERROR")
            null
        }
    }

    /**
     * AES 解码为 String
     * @param str 传入的AES加密的数据
     * @param key AES 解密的key
     * @param transformation AES加密的方式
     * @param iv ECB模式的偏移向量
     */

    fun aesDecodeToString(
        str: String, key: String, transformation: String, iv: String
    ): String? {
        return aesDecodeToByteArray(str, key, transformation, iv)?.let { String(it) }
    }

    /**
     * 已经base64的AES 解码为 ByteArray
     * @param str 传入的AES Base64加密的数据
     * @param key AES 解密的key
     * @param transformation AES加密的方式
     * @param iv ECB模式的偏移向量
     */

    fun aesBase64DecodeToByteArray(
        str: String, key: String, transformation: String, iv: String
    ): ByteArray? {
        return try {
            EncoderUtils.decryptBase64AES(
                str.encodeToByteArray(),
                key.encodeToByteArray(),
                transformation,
                iv.encodeToByteArray()
            )
        } catch (e: Exception) {
            Timber.e(e)
            log(e.localizedMessage ?: "aesDecodeToByteArrayERROR")
            null
        }
    }

    /**
     * 已经base64的AES 解码为 String
     * @param str 传入的AES Base64加密的数据
     * @param key AES 解密的key
     * @param transformation AES加密的方式
     * @param iv ECB模式的偏移向量
     */

    fun aesBase64DecodeToString(
        str: String, key: String, transformation: String, iv: String
    ): String? {
        return aesBase64DecodeToByteArray(str, key, transformation, iv)?.let { String(it) }
    }

    /**
     * 加密aes为ByteArray
     * @param data 传入的原始数据
     * @param key AES加密的key
     * @param transformation AES加密的方式
     * @param iv ECB模式的偏移向量
     */
    fun aesEncodeToByteArray(
        data: String, key: String, transformation: String, iv: String
    ): ByteArray? {
        return try {
            EncoderUtils.encryptAES(
                data.encodeToByteArray(),
                key = key.encodeToByteArray(),
                transformation,
                iv.encodeToByteArray()
            )
        } catch (e: Exception) {
            Timber.e(e)
            log(e.localizedMessage ?: "aesEncodeToByteArrayERROR")
            null
        }
    }

    /**
     * 加密aes为String
     * @param data 传入的原始数据
     * @param key AES加密的key
     * @param transformation AES加密的方式
     * @param iv ECB模式的偏移向量
     */
    fun aesEncodeToString(
        data: String, key: String, transformation: String, iv: String
    ): String? {
        return aesEncodeToByteArray(data, key, transformation, iv)?.let { String(it) }
    }

    /**
     * 加密aes后Base64化的ByteArray
     * @param data 传入的原始数据
     * @param key AES加密的key
     * @param transformation AES加密的方式
     * @param iv ECB模式的偏移向量
     */
    fun aesEncodeToBase64ByteArray(
        data: String, key: String, transformation: String, iv: String
    ): ByteArray? {
        return try {
            EncoderUtils.encryptAES2Base64(
                data.encodeToByteArray(),
                key.encodeToByteArray(),
                transformation,
                iv.encodeToByteArray()
            )
        } catch (e: Exception) {
            Timber.e(e)
            log(e.localizedMessage ?: "aesEncodeToBase64ByteArrayERROR")
            null
        }
    }

    /**
     * 加密aes后Base64化的String
     * @param data 传入的原始数据
     * @param key AES加密的key
     * @param transformation AES加密的方式
     * @param iv ECB模式的偏移向量
     */
    fun aesEncodeToBase64String(
        data: String, key: String, transformation: String, iv: String
    ): String? {
        return aesEncodeToBase64ByteArray(data, key, transformation, iv)?.let { String(it) }
    }

    fun android(): String {
        return AppConst.androidId
    }

}
