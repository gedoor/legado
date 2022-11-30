package io.legado.app.help

import android.net.Uri
import androidx.annotation.Keep
import cn.hutool.core.codec.Base64
import cn.hutool.core.util.HexUtil
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppConst.dateFormat
import io.legado.app.constant.AppLog
import io.legado.app.data.entities.BaseSource
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.http.BackstageWebView
import io.legado.app.help.http.CookieStore
import io.legado.app.help.http.SSLHelper
import io.legado.app.help.http.StrResponse
import io.legado.app.help.source.SourceVerificationHelp
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * js扩展类, 在js中通过java变量调用
 * 添加方法，请更新文档/legado/app/src/main/assets/help/JsHelp.md
 * 所有对于文件的读写删操作都是相对路径,只能操作阅读缓存内的文件
 * /android/data/{package}/cache/...
 */
@Keep
@Suppress("unused")
interface JsExtensions : JsEncodeUtils {

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
                AppLog.put("ajax(${urlStr}) error\n${it.localizedMessage}", it)
            }.getOrElse {
                it.stackTraceStr
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
                AppLog.put("connect(${urlStr}) error\n${it.localizedMessage}", it)
            }.getOrElse {
                StrResponse(analyzeUrl.url, it.stackTraceStr)
            }
        }
    }

    fun connect(urlStr: String, header: String?): StrResponse {
        return runBlocking {
            val headerMap = GSON.fromJsonObject<Map<String, String>>(header).getOrNull()
            val analyzeUrl = AnalyzeUrl(urlStr, headerMapF = headerMap, source = getSource())
            kotlin.runCatching {
                analyzeUrl.getStrResponseAwait()
            }.onFailure {
                AppLog.put("ajax($urlStr,$header) error\n${it.localizedMessage}", it)
            }.getOrElse {
                StrResponse(analyzeUrl.url, it.stackTraceStr)
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
                javaScript = js,
                headerMap = getSource()?.getHeaderMap(true),
                tag = getSource()?.getKey()
            ).getStrResponse().body
        }
    }

    /**
     * 使用内置浏览器打开链接，手动验证网站防爬
     * @param url 要打开的链接
     * @param title 浏览器页面的标题
     */
    fun startBrowser(url: String, title: String) {
        SourceVerificationHelp.startBrowser(getSource(), url, title)
    }

    /**
     * 使用内置浏览器打开链接，并等待网页结果
     */
    fun startBrowserAwait(url: String, title: String): StrResponse {
        return StrResponse(
            url,
            SourceVerificationHelp.getVerificationResult(getSource(), url, title, true)
        )
    }

    /**
     * 打开图片验证码对话框，等待返回验证结果
     */
    fun getVerificationCode(imageUrl: String): String {
        return SourceVerificationHelp.getVerificationResult(getSource(), imageUrl, "", false)
    }

    /**
     * 可从网络，本地文件(阅读私有缓存目录和书籍保存位置支持相对路径)导入JavaScript脚本
     */
    fun importScript(path: String): String {
        val result = when {
            path.startsWith("http") -> cacheFile(path) ?: ""
            path.isUri() -> Uri.parse(path).readText(appCtx)
            path.startsWith("/storage") -> FileUtils.readText(path)
            else -> readTxtFile(path)
        }
        if (result.isBlank()) throw NoStackTraceException("$path 内容获取失败或者为空")
        return result
    }

    /**
     * 缓存以文本方式保存的文件 如.js .txt等
     * @param urlStr 网络文件的链接
     * @return 返回缓存后的文件内容
     */
    fun cacheFile(urlStr: String): String? {
        return cacheFile(urlStr, 0)
    }

    /**
     * 缓存以文本方式保存的文件 如.js .txt等
     * @param saveTime 缓存时间，单位：秒
     */
    fun cacheFile(urlStr: String, saveTime: Int): String? {
        val key = md5Encode16(urlStr)
        val cache = CacheManager.getFile(key)
        if (cache.isNullOrBlank()) {
            log("首次下载 $urlStr")
            val value = ajax(urlStr) ?: return null
            CacheManager.putFile(key, value, saveTime)
            return value
        }
        return cache
    }

    /**
     *js实现读取cookie
     */
    fun getCookie(tag: String): String {
        return getCookie(tag, null)
    }

    fun getCookie(tag: String, key: String?): String {
        return if (key != null) {
            CookieStore.getKey(tag, key)
        } else {
            CookieStore.getCookie(tag)
        }
    }

    /**
     * 下载文件
     * @param url 下载地址:可带参数type,文件后缀,不带默认zip
     * @return 下载的文件相对路径
     */
    fun downloadFile(url: String): String {
        val analyzeUrl = AnalyzeUrl(url, source = getSource())
        val type = analyzeUrl.type ?: "zip"
        val path = FileUtils.getPath(
            FileUtils.createFolderIfNotExist(FileUtils.getCachePath()),
            "${MD5Utils.md5Encode16(url)}.${type}"
        )
        FileUtils.delete(path)
        analyzeUrl.getInputStream().use { iStream ->
            val file = FileUtils.createFileIfNotExist(path)
            FileOutputStream(file).use { oStream ->
                iStream.copyTo(oStream)
            }
        }
        return path.substring(FileUtils.getCachePath().length)
    }


    /**
     * 实现16进制字符串转文件
     * @param content 需要转成文件的16进制字符串
     * @param url 通过url里的参数来判断文件类型
     * @return 相对路径
     */
    fun downloadFile(content: String, url: String): String {
        val type = AnalyzeUrl(url, source = getSource()).type ?: return ""
        val path = FileUtils.getPath(
            FileUtils.createFolderIfNotExist(FileUtils.getCachePath()),
            "${MD5Utils.md5Encode16(url)}.${type}"
        )
        FileUtils.delete(path)
        val file = FileUtils.createFileIfNotExist(path)
        HexUtil.decodeHex(content).let {
            if (it.isNotEmpty()) {
                file.writeBytes(it)
            }
        }
        return path.substring(FileUtils.getCachePath().length)
    }

    /**
     * js实现重定向拦截,网络访问get
     */
    fun get(urlStr: String, headers: Map<String, String>): Connection.Response {
        val response = Jsoup.connect(urlStr)
            .sslSocketFactory(SSLHelper.unsafeSSLSocketFactory)
            .ignoreContentType(true)
            .followRedirects(false)
            .headers(headers)
            .method(Connection.Method.GET)
            .execute()
        val cookies = response.cookies()
        CookieStore.mapToCookie(cookies)?.let {
            val domain = NetworkUtils.getSubDomain(urlStr)
            CacheManager.putMemory("${domain}_cookieJar", it)
        }
        return response
    }

    /**
     * js实现重定向拦截,网络访问head,不返回Response Body更省流量
     */
    fun head(urlStr: String, headers: Map<String, String>): Connection.Response {
        val response = Jsoup.connect(urlStr)
            .sslSocketFactory(SSLHelper.unsafeSSLSocketFactory)
            .ignoreContentType(true)
            .followRedirects(false)
            .headers(headers)
            .method(Connection.Method.HEAD)
            .execute()
        val cookies = response.cookies()
        CookieStore.mapToCookie(cookies)?.let {
            val domain = NetworkUtils.getSubDomain(urlStr)
            CacheManager.putMemory("${domain}_cookieJar", it)
        }
        return response
    }

    /**
     * 网络访问post
     */
    fun post(urlStr: String, body: String, headers: Map<String, String>): Connection.Response {
        val response = Jsoup.connect(urlStr)
            .sslSocketFactory(SSLHelper.unsafeSSLSocketFactory)
            .ignoreContentType(true)
            .followRedirects(false)
            .requestBody(body)
            .headers(headers)
            .method(Connection.Method.POST)
            .execute()
        val cookies = response.cookies()
        CookieStore.mapToCookie(cookies)?.let {
            val domain = NetworkUtils.getSubDomain(urlStr)
            CacheManager.putMemory("${domain}_cookieJar", it)
        }
        return response
    }
    
    /* Str转ByteArray */
    fun strToBytes(str: String): ByteArray {
        return str.toByteArray(charset("UTF-8"))
    }
    
    fun strToBytes(str: String, charset: String): ByteArray {
        return str.toByteArray(charset(charset))
    }
    
    /* ByteArray转Str */
    fun bytesToStr(bytes: ByteArray): String {
        return String(bytes, charset("UTF-8"))
    }
    
    fun bytesToStr(bytes: ByteArray, charset: String): String {
        return String(bytes, charset(charset))
    }

    /**
     * js实现base64解码,不能删
     */
    fun base64Decode(str: String?): String {
        return Base64.decodeStr(str)
    }
    
    fun base64Decode(str: String?, charset: String): String {
        return Base64.decodeStr(str, charset(charset))
    }

    fun base64Decode(str: String, flags: Int): String {
        return EncoderUtils.base64Decode(str, flags)
    }

    fun base64DecodeToByteArray(str: String?): ByteArray? {
        if (str.isNullOrBlank()) {
            return null
        }
        return EncoderUtils.base64DecodeToByteArray(str, 0)
    }

    fun base64DecodeToByteArray(str: String?, flags: Int): ByteArray? {
        if (str.isNullOrBlank()) {
            return null
        }
        return EncoderUtils.base64DecodeToByteArray(str, flags)
    }

    fun base64Encode(str: String): String? {
        return EncoderUtils.base64Encode(str, 2)
    }

    fun base64Encode(str: String, flags: Int): String? {
        return EncoderUtils.base64Encode(str, flags)
    }

    /* HexString 解码为字节数组 */
    fun hexDecodeToByteArray(hex: String): ByteArray? {
        return HexUtil.decodeHex(hex)
    }

    /* hexString 解码为utf8String*/
    fun hexDecodeToString(hex: String): String? {
        return HexUtil.decodeHexStr(hex)
    }

    /* utf8 编码为hexString */
    fun hexEncodeToString(utf8: String): String? {
        return HexUtil.encodeHexStr(utf8)
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
        FileUtils.delete(unzipPath)
        val zipFile = getFile(zipPath)
        val unzipFolder = FileUtils.createFolderIfNotExist(unzipPath)
        ZipUtils.unzipFile(zipFile, unzipFolder)
        FileUtils.delete(zipFile.absolutePath)
        return unzipPath.substring(FileUtils.getCachePath().length)
    }

    /**
     * js实现文件夹内所有文本文件读取
     * @param path 文件夹相对路径
     * @return 所有文件字符串换行连接
     */
    fun getTxtInFolder(path: String): String {
        if (path.isEmpty()) return ""
        val folder = getFile(path)
        val contents = StringBuilder()
        folder.listFiles().let {
            if (it != null) {
                for (f in it) {
                    val charsetName = EncodingDetect.getEncode(f)
                    contents.append(String(f.readBytes(), charset(charsetName)))
                        .append("\n")
                }
                contents.deleteCharAt(contents.length - 1)
            }
        }
        FileUtils.delete(folder.absolutePath)
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
        val bytes = if (url.isAbsUrl()) {
            AnalyzeUrl(url, source = getSource()).getByteArray()
        } else {
            HexUtil.decodeHex(url)
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
        try {
            val key = md5Encode16(str)
            var qTTF = CacheManager.getQueryTTF(key)
            if (qTTF != null) return qTTF
            val font: ByteArray? = when {
                str.isAbsUrl() -> runBlocking {
                    var x = CacheManager.getByteArray(key)
                    if (x == null) {
                        x = AnalyzeUrl(str, source = getSource()).getByteArray()
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
        } catch (e: Exception) {
            AppLog.put("获取字体处理类出错", e)
            throw e
        }
    }

    /**
     * @param text 包含错误字体的内容
     * @param errorQueryTTF 错误的字体
     * @param correctQueryTTF 正确的字体
     */
    fun replaceFont(
        text: String,
        errorQueryTTF: QueryTTF?,
        correctQueryTTF: QueryTTF?
    ): String {
        if (errorQueryTTF == null || correctQueryTTF == null) return text
        val contentArray = text.toStringArray() //这里不能用toCharArray,因为有些文字占多个字节
        contentArray.forEachIndexed { index, s ->
            val oldCode = s.codePointAt(0)
            if (errorQueryTTF.inLimit(oldCode)) {
                val glyf = errorQueryTTF.getGlyfByCode(oldCode)
                val code = correctQueryTTF.getCodeByGlyf(glyf)
                if (code != 0) {
                    contentArray[index] = code.toChar().toString()
                }
            }
        }
        return contentArray.joinToString("")
    }

    /**
     * 弹窗提示
     */
    fun toast(msg: Any?) {
        appCtx.toastOnUi("${getSource()?.getTag()}: ${msg.toString()}")
    }

    /**
     * 弹窗提示 停留时间较长
     */
    fun longToast(msg: Any?) {
        appCtx.longToastOnUi("${getSource()?.getTag()}: ${msg.toString()}")
    }

    /**
     * 输出调试日志
     */
    fun log(msg: Any?): Any? {
        getSource()?.let {
            Debug.log(it.getKey(), msg.toString())
        } ?: Debug.log(msg.toString())
        AppLog.putDebug(msg.toString())
        return msg
    }

    /**
     * 输出对象类型
     */
    fun logType(any: Any?) {
        if (any == null) {
            log("null")
        } else {
            log(any.javaClass.name)
        }
    }

    /**
     * 生成UUID
     */
    fun randomUUID(): String {
        return UUID.randomUUID().toString()
    }

    fun androidId(): String {
        return AppConst.androidId
    }

}
