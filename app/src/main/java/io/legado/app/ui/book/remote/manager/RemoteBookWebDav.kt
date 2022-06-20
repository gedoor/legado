package io.legado.app.ui.book.remote.manager


import android.net.Uri
import io.legado.app.constant.AppPattern.bookFileRegex
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.AppWebDav
import io.legado.app.lib.webdav.WebDav
import io.legado.app.lib.webdav.WebDavFile
import io.legado.app.model.localBook.LocalBook
import io.legado.app.ui.book.remote.RemoteBook
import io.legado.app.ui.book.remote.RemoteBookManager
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.readBytes
import kotlinx.coroutines.runBlocking
import splitties.init.appCtx
import java.io.File
import java.net.URLDecoder

object RemoteBookWebDav : RemoteBookManager() {
    private val remoteBookUrl get() = "${AppWebDav.rootWebDavUrl}${remoteBookFolder}"

    init {
        runBlocking {
            initRemoteContext()
        }
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
            val remoteWebDavFileList: List<WebDavFile> = WebDav(remoteBookUrl, it).listFiles()
            //转化远程文件信息到本地对象
            remoteWebDavFileList.forEach { webDavFile ->
                var webDavFileName = webDavFile.displayName
                webDavFileName = URLDecoder.decode(webDavFileName, "utf-8")

                if (webDavFile.isDir) {
                    remoteBooks.add(
                        RemoteBook(
                            webDavFileName, webDavFile.path, webDavFile.size,
                            "folder", webDavFile.lastModify, false
                        )
                    )
                } else {
                    //分割后缀
                    val fileExtension = webDavFileName.substringAfterLast(".")

                    //扩展名符合阅读的格式则认为是书籍
                    if (bookFileRegex.matches(webDavFileName)) {
                        val isOnBookShelf = LocalBook.isOnBookShelf(webDavFileName)
                        remoteBooks.add(
                            RemoteBook(
                                webDavFileName, webDavFile.path, webDavFile.size,
                                fileExtension, webDavFile.lastModify, isOnBookShelf
                            )
                        )
                    }
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
            val webdav = WebDav(remoteBook.path, it)
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