package io.legado.app.lib.webdav

import io.legado.app.help.http.HttpHelper
import io.legado.app.utils.await
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.util.*

@Suppress("unused", "MemberVisibilityCanBePrivate")
class WebDav(urlStr: String) {
    companion object {
        // 指定返回哪些属性
        private const val DIR =
            """<?xml version="1.0"?>
                <a:propfind xmlns:a="DAV:">
                    <a:prop>
                        <a:displayname/>
                        <a:resourcetype/>
                        <a:getcontentlength/>
                        <a:creationdate/>
                        <a:getlastmodified/>
                        %s
                    </a:prop>
                </a:propfind>"""
    }

    private val url: URL = URL(urlStr)
    private val httpUrl: String? by lazy {
        val raw = url.toString().replace("davs://", "https://").replace("dav://", "http://")
        return@lazy kotlin.runCatching {
            URLEncoder.encode(raw, "UTF-8")
                .replace("\\+".toRegex(), "%20")
                .replace("%3A".toRegex(), ":")
                .replace("%2F".toRegex(), "/")
        }.getOrNull()
    }
    val host: String? get() = url.host
    val path get() = url.toString()
    var displayName: String? = null
    var size: Long = 0
    var exists = false
    var parent = ""
    var urlName = ""
    var contentType = ""

    /**
     * 填充文件信息。实例化WebDAVFile对象时，并没有将远程文件的信息填充到实例中。需要手动填充！
     * @return 远程文件是否存在
     */
    suspend fun indexFileInfo(): Boolean {
        propFindResponse(ArrayList())?.let { response ->
            if (!response.isSuccessful) {
                this.exists = false
                return false
            }
            response.body?.let {
                @Suppress("BlockingMethodInNonBlockingContext")
                if (it.string().isNotEmpty()) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 列出当前路径下的文件
     *
     * @param propsList 指定列出文件的哪些属性
     * @return 文件列表
     */
    suspend fun listFiles(propsList: ArrayList<String> = ArrayList()): List<WebDav> {
        propFindResponse(propsList)?.let { response ->
            if (response.isSuccessful) {
                response.body?.let { body ->
                    @Suppress("BlockingMethodInNonBlockingContext")
                    return parseDir(body.string())
                }
            }
        }
        return ArrayList()
    }

    @Throws(IOException::class)
    private suspend fun propFindResponse(propsList: ArrayList<String>, depth: Int = 1): Response? {
        val requestProps = StringBuilder()
        for (p in propsList) {
            requestProps.append("<a:").append(p).append("/>\n")
        }
        val requestPropsStr: String = if (requestProps.toString().isEmpty()) {
            DIR.replace("%s", "")
        } else {
            String.format(DIR, requestProps.toString() + "\n")
        }
        httpUrl?.let { url ->
            // 添加RequestBody对象，可以只返回的属性。如果设为null，则会返回全部属性
            // 注意：尽量手动指定需要返回的属性。若返回全部属性，可能后由于Prop.java里没有该属性名，而崩溃。
            val requestBody = requestPropsStr.toRequestBody("text/plain".toMediaType())
            val request = Request.Builder()
                .url(url)
                .method("PROPFIND", requestBody)

            HttpAuth.auth?.let {
                request.header("Authorization", Credentials.basic(it.user, it.pass))
            }
            request.header("Depth", if (depth < 0) "infinity" else depth.toString())
            return HttpHelper.client.newCall(request.build()).await()
        }
        return null
    }

    private fun parseDir(s: String): List<WebDav> {
        val list = ArrayList<WebDav>()
        val document = Jsoup.parse(s)
        val elements = document.getElementsByTag("d:response")
        httpUrl?.let { urlStr ->
            val baseUrl = if (urlStr.endsWith("/")) urlStr else "$urlStr/"
            for (element in elements) {
                val href = element.getElementsByTag("d:href")[0].text()
                if (!href.endsWith("/")) {
                    val fileName = href.substring(href.lastIndexOf("/") + 1)
                    val webDavFile: WebDav
                    try {
                        webDavFile = WebDav(baseUrl + fileName)
                        webDavFile.displayName = fileName
                        webDavFile.contentType = element
                            .getElementsByTag("d:getcontenttype")
                            .getOrNull(0)?.text() ?: ""
                        if (href.isEmpty()) {
                            webDavFile.urlName =
                                if (parent.isEmpty()) url.file.replace("/", "")
                                else url.toString().replace(parent, "").replace("/", "")
                        } else {
                            webDavFile.urlName = href
                        }
                        list.add(webDavFile)
                    } catch (e: MalformedURLException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return list
    }

    /**
     * 根据自己的URL，在远程处创建对应的文件夹
     *
     * @return 是否创建成功
     */
    suspend fun makeAsDir(): Boolean {
        httpUrl?.let { url ->
            val request = Request.Builder()
                .url(url)
                .method("MKCOL", null)
            return execRequest(request)
        }
        return false
    }

    /**
     * 下载到本地
     *
     * @param savedPath       本地的完整路径，包括最后的文件名
     * @param replaceExisting 是否替换本地的同名文件
     * @return 下载是否成功
     */
    suspend fun downloadTo(savedPath: String, replaceExisting: Boolean): Boolean {
        if (File(savedPath).exists()) {
            if (!replaceExisting) return false
        }
        val inputS = getInputStream() ?: return false
        File(savedPath).writeBytes(inputS.readBytes())
        return true
    }

    suspend fun download(): ByteArray? {
        val inputS = getInputStream() ?: return null
        return inputS.readBytes()
    }

    /**
     * 上传文件
     */
    suspend fun upload(localPath: String, contentType: String? = null): Boolean {
        val file = File(localPath)
        if (!file.exists()) return false
        // 务必注意RequestBody不要嵌套，不然上传时内容可能会被追加多余的文件信息
        val fileBody = file.asRequestBody(contentType?.toMediaType())
        httpUrl?.let {
            val request = Request.Builder()
                .url(it)
                .put(fileBody)
            return execRequest(request)
        }
        return false
    }

    suspend fun upload(byteArray: ByteArray, contentType: String? = null): Boolean {
        // 务必注意RequestBody不要嵌套，不然上传时内容可能会被追加多余的文件信息
        val fileBody = byteArray.toRequestBody(contentType?.toMediaType())
        httpUrl?.let {
            val request = Request.Builder()
                .url(it)
                .put(fileBody)
            return execRequest(request)
        }
        return false
    }

    /**
     * 执行请求，获取响应结果
     * @param requestBuilder 因为还需要追加验证信息，所以此处传递Request.Builder的对象，而不是Request的对象
     * @return 请求执行的结果
     */
    @Throws(IOException::class)
    private suspend fun execRequest(requestBuilder: Request.Builder): Boolean {
        HttpAuth.auth?.let {
            requestBuilder.header("Authorization", Credentials.basic(it.user, it.pass))
        }
        val response = HttpHelper.client.newCall(requestBuilder.build()).await()
        return response.isSuccessful
    }

    @Throws(IOException::class)
    private suspend fun getInputStream(): InputStream? {
        val url = httpUrl
        val auth = HttpAuth.auth
        if (url != null && auth != null) {
            return RxHttp.get(url)
                .addHeader("Authorization", Credentials.basic(auth.user, auth.pass))
                .toInputStream().await()
        }
        return null
    }

}