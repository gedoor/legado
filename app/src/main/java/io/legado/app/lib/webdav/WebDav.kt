package io.legado.app.lib.webdav

import io.legado.app.lib.webdav.http.Handler
import io.legado.app.lib.webdav.http.HttpAuth
import io.legado.app.lib.webdav.http.OkHttp
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.util.*

class WebDav @Throws(MalformedURLException::class)
constructor(url: String) {
    private val url: URL = URL(null, url, Handler.HANDLER)
    private var httpUrl: String? = null

    var displayName: String? = null
    var size: Long = 0
    private var exists = false
    var parent = ""
    var urlName = ""
        get() {
            if (field.isEmpty()) {
                this.urlName = (if (parent.isEmpty()) url.file else url.toString().replace(parent, "")).replace("/", "")
            }
            return field
        }

    private val okHttpClient: OkHttpClient = OkHttp.okHttpClient

    val path: String
        get() = url.toString()

    private val inputStream: InputStream?
        get() = getUrl()?.let { url ->
            val request = Request.Builder().url(url)
            HttpAuth.auth?.let { request.header("Authorization", Credentials.basic(it.user, it.pass)) }

            try {
                return okHttpClient.newCall(request.build()).execute().body?.byteStream()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
            return null
        }

    val host: String
        get() = url.host

    private fun getUrl(): String? {
        if (httpUrl == null) {
            val raw = url.toString().replace("davs://", "https://").replace("dav://", "http://")
            try {
                httpUrl = URLEncoder.encode(raw, "UTF-8")
                    .replace("\\+".toRegex(), "%20")
                    .replace("%3A".toRegex(), ":")
                    .replace("%2F".toRegex(), "/")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

        }
        return httpUrl
    }

    /**
     * 填充文件信息。实例化WebDAVFile对象时，并没有将远程文件的信息填充到实例中。需要手动填充！
     *
     * @return 远程文件是否存在
     */
    @Throws(IOException::class)
    fun indexFileInfo(): Boolean {
        propFindResponse(ArrayList())?.let { response ->
            if (!response.isSuccessful) {
                this.exists = false
                return false
            }
            response.body?.let {
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
    @Throws(IOException::class)
    @JvmOverloads
    fun listFiles(propsList: ArrayList<String> = ArrayList()): List<WebDav> {
        propFindResponse(propsList)?.let { response ->
            if (response.isSuccessful) {
                response.body?.let { body ->
                    return parseDir(body.string())
                }
            }

        }
        return ArrayList()
    }

    @Throws(IOException::class)
    private fun propFindResponse(propsList: ArrayList<String>, depth: Int = 1): Response? {
        val requestProps = StringBuilder()
        for (p in propsList) {
            requestProps.append("<a:").append(p).append("/>\n")
        }
        val requestPropsStr: String
        requestPropsStr = if (requestProps.toString().isEmpty()) {
            DIR.replace("%s", "")
        } else {
            String.format(DIR, requestProps.toString() + "\n")
        }
        getUrl()?.let { url ->
            val request = Request.Builder()
                .url(url)
                // 添加RequestBody对象，可以只返回的属性。如果设为null，则会返回全部属性
                // 注意：尽量手动指定需要返回的属性。若返回全部属性，可能后由于Prop.java里没有该属性名，而崩溃。
                .method("PROPFIND", RequestBody.create("text/plain".toMediaTypeOrNull(), requestPropsStr))

            HttpAuth.auth?.let { request.header("Authorization", Credentials.basic(it.user, it.pass)) }

            request.header("Depth", if (depth < 0) "infinity" else Integer.toString(depth))

            return okHttpClient.newCall(request.build()).execute()
        }
        return null
    }

    private fun parseDir(s: String): List<WebDav> {
        val list = ArrayList<WebDav>()
        val document = Jsoup.parse(s)
        val elements = document.getElementsByTag("d:response")
        getUrl()?.let { url ->
            val baseUrl = if (url.endsWith("/")) url else "$url/"
            for (element in elements) {
                val href = element.getElementsByTag("d:href")[0].text()
                if (!href.endsWith("/")) {
                    val fileName = href.substring(href.lastIndexOf("/") + 1)
                    val webDavFile: WebDav
                    try {
                        webDavFile = WebDav(baseUrl + fileName)
                        webDavFile.displayName = fileName
                        webDavFile.urlName = href
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
    @Throws(IOException::class)
    fun makeAsDir(): Boolean {
        getUrl()?.let { url ->
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
    fun downloadTo(savedPath: String, replaceExisting: Boolean): Boolean {
        if (File(savedPath).exists()) {
            if (!replaceExisting) {
                return false
            }
        }
        val inputS = inputStream ?: return false
        File(savedPath).writeBytes(inputS.readBytes())
        return true
    }

    /**
     * 上传文件
     */
    @Throws(IOException::class)
    @JvmOverloads
    fun upload(localPath: String, contentType: String? = null): Boolean {
        val file = File(localPath)
        if (!file.exists()) return false
        val mediaType = contentType?.toMediaTypeOrNull()
        // 务必注意RequestBody不要嵌套，不然上传时内容可能会被追加多余的文件信息
        val fileBody = RequestBody.create(mediaType, file)
        getUrl()?.let {
            val request = Request.Builder()
                .url(it)
                .put(fileBody)
            return execRequest(request)
        }
        return false
    }

    /**
     * 执行请求，获取响应结果
     *
     * @param requestBuilder 因为还需要追加验证信息，所以此处传递Request.Builder的对象，而不是Request的对象
     * @return 请求执行的结果
     */
    @Throws(IOException::class)
    private fun execRequest(requestBuilder: Request.Builder): Boolean {
        HttpAuth.auth?.let { requestBuilder.header("Authorization", Credentials.basic(it.user, it.pass)) }

        val response = okHttpClient.newCall(requestBuilder.build()).execute()
        return response.isSuccessful
    }

    fun canRead(): Boolean {
        return true
    }

    fun canWrite(): Boolean {
        return false
    }

    fun exists(): Boolean {
        return exists
    }

    fun exists(exists: Boolean) {
        this.exists = exists
    }

    companion object {
        val TAG = WebDav::class.java.simpleName
        val OBJECT_NOT_EXISTS_TAG = "ObjectNotFound"
        // 指定返回哪些属性
        private val DIR = "<?xml version=\"1.0\"?>\n" +
                "<a:propfind xmlns:a=\"DAV:\">\n" +
                "<a:prop>\n" +
                "<a:displayname/>\n<a:resourcetype/>\n<a:getcontentlength/>\n<a:creationdate/>\n<a:getlastmodified/>\n%s" +
                "</a:prop>\n" +
                "</a:propfind>"

        /**
         * 打印对象内的所有属性
         */
        fun <T> printAllAttrs(className: String, o: Any): T? {
            try {
                val c = Class.forName(className)
                val fields = c.declaredFields
                for (f in fields) {
                    f.isAccessible = true
                }
                println("=============$className===============")
                for (f in fields) {
                    val field = f.toString().substring(f.toString().lastIndexOf(".") + 1) //取出属性名称
                    println(field + " --> " + f.get(o))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }
    }
}