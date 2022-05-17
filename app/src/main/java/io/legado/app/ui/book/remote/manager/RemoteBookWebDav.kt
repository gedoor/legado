package io.legado.app.ui.book.remote.manager


import android.net.Uri
import io.legado.app.constant.PreferKey

import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.config.AppConfig

import io.legado.app.lib.webdav.Authorization
import io.legado.app.lib.webdav.WebDav
import io.legado.app.lib.webdav.WebDavException
import io.legado.app.lib.webdav.WebDavFile
import io.legado.app.ui.book.info.BookInfoActivity

import io.legado.app.ui.book.remote.RemoteBook
import io.legado.app.ui.book.remote.RemoteBookManager
import io.legado.app.utils.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import splitties.init.appCtx
import java.io.File
import java.nio.charset.Charset

object RemoteBookWebDav : RemoteBookManager() {
    private const val defaultWebDavUrl = "https://dav.jianguoyun.com/dav/"
    private var authorization: Authorization? = null
    private val remoteBookUrl get() = "${rootWebDavUrl}${remoteBookFolder}"
    private val localSaveFolder get() = "${appCtx.externalFiles.absolutePath}${File.separator}${remoteBookFolder}"
    init {
        runBlocking {
            initRemoteContext()
        }
    }

    private val rootWebDavUrl: String
        get() {
            val configUrl = appCtx.getPrefString(PreferKey.webDavUrl)?.trim()
            var url = if (configUrl.isNullOrEmpty()) defaultWebDavUrl else configUrl
            if (!url.endsWith("/")) url = "${url}/"
            AppConfig.webDavDir?.trim()?.let {
                if (it.isNotEmpty()) {
                    url = "${url}${it}/"
                }
            }
            return url
        }

    override suspend fun initRemoteContext() {
        kotlin.runCatching {
            authorization = null
            val account = appCtx.getPrefString(PreferKey.webDavAccount)
            val password = appCtx.getPrefString(PreferKey.webDavPassword)
            if (!account.isNullOrBlank() && !password.isNullOrBlank()) {
                val mAuthorization = Authorization(account, password)
                WebDav(rootWebDavUrl, mAuthorization).makeAsDir()
                WebDav(remoteBookUrl, mAuthorization).makeAsDir()
                authorization = mAuthorization
            }
        }.onFailure {
            it.printStackTrace()
        }
    }

    @Throws(Exception::class)
    override suspend fun getRemoteBookList(): MutableList<RemoteBook> {
        val remoteBooks = mutableListOf<RemoteBook>()
        authorization?.let {
            //读取文件列表
            var remoteWebDavFileList : List<WebDavFile>? = null
            kotlin.runCatching {
                remoteWebDavFileList = WebDav(remoteBookUrl, it).listFiles()
            }
            //逆序文件排序
            remoteWebDavFileList = remoteWebDavFileList!!.reversed()
            //转化远程文件信息到本地对象
            remoteWebDavFileList!!.forEach { webDavFile ->
                val webDavFileName = webDavFile.displayName
                val webDavUrlName = "${remoteBookUrl}${File.separator}${webDavFile.displayName}"

                // 转码
                //val trueFileName = String(webDavFileName.toByteArray(Charset.forName("GBK")), Charset.forName("UTF-8"))
                //val trueUrlName = String(webDavUrlName.toByteArray(Charset.forName("GBK")), Charset.forName("UTF-8"))

                //分割后缀
                val fileExtension = webDavFileName.substringAfterLast(".")

                //扩展名符合阅读的格式则认为是书籍
                if (contentTypeList.contains(fileExtension)) {
                    remoteBooks.add(RemoteBook(webDavFileName,webDavUrlName,webDavFile.size,fileExtension,webDavFile.lastModify))
                }
            }
        } ?: throw NoStackTraceException("webDav没有配置")
        return remoteBooks
    }

    override suspend fun getRemoteBook(remoteBook: RemoteBook): String? {
        val saveFilePath= "${localSaveFolder}${File.separator}${remoteBook.filename}"
        kotlin.runCatching {
            authorization?.let {
                FileUtils.createFolderIfNotExist(localSaveFolder).run{
                        val webdav = WebDav(
                            remoteBook.urlName,
                            it
                        )
                        webdav.downloadTo(saveFilePath, true)
                }
            }
        }.onFailure {
            it.printStackTrace()
            return null
        }
        return saveFilePath
    }

    /**
     * 上传本地导入的书籍到远程
     */
    override suspend fun upload(localBookUri: Uri): Boolean {
        if (!NetworkUtils.isAvailable()) return false

        val localBookName = localBookUri.path?.substringAfterLast(File.separator)
        val putUrl = "${remoteBookUrl}${File.separator}${localBookName}"
        kotlin.runCatching {
            authorization?.let {
                if (localBookUri.isContentScheme()){
                    WebDav(putUrl, it).upload(byteArray = localBookUri.readBytes(appCtx),contentType = "application/octet-stream")
                }else{
                    WebDav(putUrl, it).upload(localBookUri.path!!)
                }
            }
        }.onFailure {
            return false
        }
        return true
    }

    override suspend fun delete(remoteBookUrl: String): Boolean {
        TODO("Not yet implemented")
    }

//    suspend fun showRestoreDialog(context: Context) {
//        val names = withContext(Dispatchers.IO) { getBackupNames() }
//        if (names.isNotEmpty()) {
//            withContext(Dispatchers.Main) {
//                context.selector(
//                    title = context.getString(R.string.select_restore_file),
//                    items = names
//                ) { _, index ->
//                    if (index in 0 until names.size) {
//                        Coroutine.async {
//                            restoreWebDav(names[index])
//                        }.onError {
//                            appCtx.toastOnUi("WebDav恢复出错\n${it.localizedMessage}")
//                        }
//                    }
//                }
//            }
//        } else {
//            throw NoStackTraceException("Web dav no back up file")
//        }
//    }
//
//    @Throws(WebDavException::class)
//    suspend fun restoreWebDav(name: String) {
//        authorization?.let {
//            val webDav = WebDav(rootWebDavUrl + name, it)
//            webDav.downloadTo(zipFilePath, true)
//            @Suppress("BlockingMethodInNonBlockingContext")
//            ZipUtils.unzipFile(zipFilePath, Backup.backupPath)
//            Restore.restoreDatabase()
//            Restore.restoreConfig()
//        }
//    }
//
//    suspend fun hasBackUp(): Boolean {
//        authorization?.let {
//            val url = "${rootWebDavUrl}${backupFileName}"
//            return WebDav(url, it).exists()
//        }
//        return false
//    }
//
//    suspend fun lastBackUp(): Result<WebDavFile?> {
//        return kotlin.runCatching {
//            authorization?.let {
//                var lastBackupFile: WebDavFile? = null
//                WebDav(rootWebDavUrl, it).listFiles().reversed().forEach { webDavFile ->
//                    if (webDavFile.displayName.startsWith("backup")) {
//                        if (lastBackupFile == null
//                            || webDavFile.lastModify > lastBackupFile!!.lastModify
//                        ) {
//                            lastBackupFile = webDavFile
//                        }
//                    }
//                }
//                lastBackupFile
//            }
//        }
//    }
//
//    @Throws(Exception::class)
//    suspend fun backUpWebDav(path: String) {
//        if (!NetworkUtils.isAvailable()) return
//        authorization?.let {
//            val paths = arrayListOf(*Backup.backupFileNames)
//            for (i in 0 until paths.size) {
//                paths[i] = path + File.separator + paths[i]
//            }
//            FileUtils.delete(zipFilePath)
//            if (ZipUtils.zipFiles(paths, zipFilePath)) {
//                val putUrl = "${rootWebDavUrl}${backupFileName}"
//                WebDav(putUrl, it).upload(zipFilePath)
//            }
//        }
//    }
//
//    suspend fun exportWebDav(byteArray: ByteArray, fileName: String) {
//        if (!NetworkUtils.isAvailable()) return
//        try {
//            authorization?.let {
//                // 如果导出的本地文件存在,开始上传
//                val putUrl = exportsWebDavUrl + fileName
//                WebDav(putUrl, it).upload(byteArray, "text/plain")
//            }
//        } catch (e: Exception) {
//            val msg = "WebDav导出\n${e.localizedMessage}"
//            AppLog.put(msg)
//            appCtx.toastOnUi(msg)
//        }
//    }
//
//    fun uploadBookProgress(book: Book) {
//        val authorization = authorization ?: return
//        if (!syncBookProgress) return
//        if (!NetworkUtils.isAvailable()) return
//        Coroutine.async {
//            val bookProgress = BookProgress(book)
//            val json = GSON.toJson(bookProgress)
//            val url = getProgressUrl(book)
//            WebDav(url, authorization).upload(json.toByteArray(), "application/json")
//        }.onError {
//            AppLog.put("上传进度失败\n${it.localizedMessage}")
//        }
//    }
//
//    private fun getProgressUrl(book: Book): String {
//        return bookProgressUrl + book.name + "_" + book.author + ".json"
//    }
//
//    /**
//     * 获取书籍进度
//     */
//    suspend fun getBookProgress(book: Book): BookProgress? {
//        authorization?.let {
//            val url = getProgressUrl(book)
//            kotlin.runCatching {
//                WebDav(url, it).download().let { byteArray ->
//                    val json = String(byteArray)
//                    if (json.isJson()) {
//                        return GSON.fromJsonObject<BookProgress>(json).getOrNull()
//                    }
//                }
//            }
//        }
//        return null
//    }
//
//    suspend fun downloadAllBookProgress() {
//        authorization ?: return
//        if (!NetworkUtils.isAvailable()) return
//        appDb.bookDao.all.forEach { book ->
//            getBookProgress(book)?.let { bookProgress ->
//                if (bookProgress.durChapterIndex > book.durChapterIndex
//                    || (bookProgress.durChapterIndex == book.durChapterIndex
//                            && bookProgress.durChapterPos > book.durChapterPos)
//                ) {
//                    book.durChapterIndex = bookProgress.durChapterIndex
//                    book.durChapterPos = bookProgress.durChapterPos
//                    book.durChapterTitle = bookProgress.durChapterTitle
//                    book.durChapterTime = bookProgress.durChapterTime
//                    appDb.bookDao.update(book)
//                }
//            }
//        }
//    }
}