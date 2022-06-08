package io.legado.app.ui.book.remote.manager


import android.net.Uri
import io.legado.app.constant.AppPattern.bookFileRegex
import io.legado.app.constant.PreferKey
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.AppWebDav
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.webdav.WebDav
import io.legado.app.lib.webdav.WebDavFile
import io.legado.app.model.localBook.LocalBook
import io.legado.app.ui.book.remote.RemoteBook
import io.legado.app.ui.book.remote.RemoteBookManager
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.getPrefString
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.readBytes
import kotlinx.coroutines.runBlocking
import splitties.init.appCtx
import java.io.File
import java.net.URLDecoder

object RemoteBookWebDav : RemoteBookManager() {
    private val remoteBookUrl get() = "${rootWebDavUrl}${remoteBookFolder}"

    init {
        runBlocking {
            initRemoteContext()
        }
    }

    private val rootWebDavUrl: String
        get() {
            val configUrl = appCtx.getPrefString(PreferKey.webDavUrl)?.trim()
            var url = if (configUrl.isNullOrEmpty()) AppWebDav.defaultWebDavUrl else configUrl
            if (!url.endsWith("/")) url = "${url}/"
            AppConfig.webDavDir?.trim()?.let {
                if (it.isNotEmpty()) {
                    url = "${url}${it}/"
                }
            }
            return url
        }

    override suspend fun initRemoteContext() {
        AppWebDav.authorization?.let {
            WebDav(remoteBookUrl, it).makeAsDir()
        }
    }

    /**
     * 获取远程书籍列表
     */
    @Throws(Exception::class)
    override suspend fun getRemoteBookList(): MutableList<RemoteBook> {
        val remoteBooks = mutableListOf<RemoteBook>()
        AppWebDav.authorization?.let {
            //读取文件列表
            var remoteWebDavFileList: List<WebDavFile>? = null
            kotlin.runCatching {
                remoteWebDavFileList = WebDav(remoteBookUrl, it).listFiles()
            }
            //逆序文件排序
            remoteWebDavFileList = remoteWebDavFileList!!.reversed()
            //转化远程文件信息到本地对象
            remoteWebDavFileList!!.forEach { webDavFile ->
                var webDavFileName = webDavFile.displayName
                var webDavUrlName = "${remoteBookUrl}${File.separator}${webDavFile.displayName}"
                webDavFileName = URLDecoder.decode(webDavFileName,"utf-8")
                webDavUrlName = URLDecoder.decode(webDavUrlName,"utf-8")
                // 转码
                //val trueFileName = String(webDavFileName.toByteArray(Charset.forName("GBK")), Charset.forName("UTF-8"))
                //val trueUrlName = String(webDavUrlName.toByteArray(Charset.forName("GBK")), Charset.forName("UTF-8"))

                //分割后缀
                val fileExtension = webDavFileName.substringAfterLast(".")

                //扩展名符合阅读的格式则认为是书籍
                if (bookFileRegex.matches(webDavFileName)) {
                    val isOnBookShelf = LocalBook.isOnBookShelf(webDavFileName)
                    remoteBooks.add(
                        RemoteBook(
                            webDavFileName, webDavUrlName, webDavFile.size,
                            fileExtension, webDavFile.lastModify, isOnBookShelf
                        )
                    )
                }
            }
        } ?: throw NoStackTraceException("webDav没有配置")
        return remoteBooks
    }

    /**
     * 下载指定的远程书籍到本地
     */
    override suspend fun getRemoteBook(remoteBook: RemoteBook): Uri? {
        return AppWebDav.authorization?.let {
            val webdav = WebDav(remoteBook.urlName, it)
            webdav.download().let { bytes ->
                LocalBook.saveBookFile(bytes, remoteBook.filename)
            }
        }
    }

    /**
     * 上传本地导入的书籍到远程
     */
    override suspend fun upload(localBookUri: Uri): Boolean {
        if (!NetworkUtils.isAvailable()) return false

        val localBookName = localBookUri.path?.substringAfterLast(File.separator)
        val putUrl = "${remoteBookUrl}${File.separator}${localBookName}"
        AppWebDav.authorization?.let {
            if (localBookUri.isContentScheme()) {
                WebDav(putUrl, it).upload(
                    byteArray = localBookUri.readBytes(appCtx),
                    contentType = "application/octet-stream"
                )
            } else {
                WebDav(putUrl, it).upload(localBookUri.path!!)
            }
        }
        return true
    }

    override suspend fun delete(remoteBookUrl: String): Boolean {
        TODO("Not yet implemented")
    }

}