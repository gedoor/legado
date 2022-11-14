package io.legado.app.help

import android.content.Context
import android.net.Uri
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookProgress
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.Restore
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.webdav.Authorization
import io.legado.app.lib.webdav.WebDav
import io.legado.app.lib.webdav.WebDavException
import io.legado.app.lib.webdav.WebDavFile
import io.legado.app.ui.widget.dialog.WaitDialog
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.coroutineContext

/**
 * webDav初始化会访问网络,不要放到主线程
 */
object AppWebDav {
    private const val defaultWebDavUrl = "https://dav.jianguoyun.com/dav/"
    private val zipFilePath = "${appCtx.externalFiles.absolutePath}${File.separator}backup.zip"
    private val bookProgressUrl get() = "${rootWebDavUrl}bookProgress/"
    private val exportsWebDavUrl get() = "${rootWebDavUrl}books/"

    var authorization: Authorization? = null
        private set

    val isOk get() = authorization != null

    init {
        runBlocking {
            upConfig()
        }
    }

    val rootWebDavUrl: String
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

    private val backupFileName: String
        get() {
            val backupDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date(System.currentTimeMillis()))
            val deviceName = AppConfig.webDavDeviceName
            return if (deviceName?.isNotBlank() == true) {
                "backup${backupDate}-${deviceName}.zip"
            } else {
                "backup${backupDate}.zip"
            }
        }

    suspend fun upConfig() {
        kotlin.runCatching {
            authorization = null
            val account = appCtx.getPrefString(PreferKey.webDavAccount)
            val password = appCtx.getPrefString(PreferKey.webDavPassword)
            if (!account.isNullOrBlank() && !password.isNullOrBlank()) {
                val mAuthorization = Authorization(account, password)
                WebDav(rootWebDavUrl, mAuthorization).makeAsDir()
                WebDav(bookProgressUrl, mAuthorization).makeAsDir()
                WebDav(exportsWebDavUrl, mAuthorization).makeAsDir()
                authorization = mAuthorization
            }
        }
    }

    @Throws(Exception::class)
    private suspend fun getBackupNames(): ArrayList<String> {
        val names = arrayListOf<String>()
        authorization?.let {
            var files = WebDav(rootWebDavUrl, it).listFiles()
            files = files.reversed()
            files.forEach { webDav ->
                val name = webDav.displayName
                if (name.startsWith("backup")) {
                    names.add(name)
                }
            }
        } ?: throw NoStackTraceException("webDav没有配置")
        return names
    }

    suspend fun showRestoreDialog(context: Context) {
        val names = withContext(IO) { getBackupNames() }
        if (names.isNotEmpty()) {
            coroutineContext.ensureActive()
            withContext(Main) {
                context.selector(
                    title = context.getString(R.string.select_restore_file),
                    items = names
                ) { _, index ->
                    if (index in 0 until names.size) {
                        val waitDialog = WaitDialog(context)
                        waitDialog.setText("恢复中…")
                        waitDialog.show()
                        val task = Coroutine.async {
                            restoreWebDav(names[index])
                        }.onError {
                            AppLog.put("WebDav恢复出错\n${it.localizedMessage}", it)
                            appCtx.toastOnUi("WebDav恢复出错\n${it.localizedMessage}")
                        }.onFinally(Main) {
                            waitDialog.dismiss()
                        }
                        waitDialog.setOnCancelListener {
                            task.cancel()
                        }
                    }
                }
            }
        } else {
            throw NoStackTraceException("Web dav no back up file")
        }
    }

    @Throws(WebDavException::class)
    suspend fun restoreWebDav(name: String) {
        authorization?.let {
            val webDav = WebDav(rootWebDavUrl + name, it)
            webDav.downloadTo(zipFilePath, true)
            ZipUtils.unzipFile(zipFilePath, Backup.backupPath)
            Restore.restoreDatabase()
            Restore.restoreConfig()
        }
    }

    suspend fun hasBackUp(): Boolean {
        authorization?.let {
            val url = "$rootWebDavUrl$backupFileName"
            return WebDav(url, it).exists()
        }
        return false
    }

    suspend fun lastBackUp(): Result<WebDavFile?> {
        return kotlin.runCatching {
            authorization?.let {
                var lastBackupFile: WebDavFile? = null
                WebDav(rootWebDavUrl, it).listFiles().reversed().forEach { webDavFile ->
                    if (webDavFile.displayName.startsWith("backup")) {
                        if (lastBackupFile == null
                            || webDavFile.lastModify > lastBackupFile!!.lastModify
                        ) {
                            lastBackupFile = webDavFile
                        }
                    }
                }
                lastBackupFile
            }
        }
    }

    @Throws(Exception::class)
    suspend fun backUpWebDav(path: String) {
        if (!NetworkUtils.isAvailable()) return
        authorization?.let {
            val paths = arrayListOf(*Backup.backupFileNames)
            for (i in 0 until paths.size) {
                paths[i] = path + File.separator + paths[i]
            }
            FileUtils.delete(zipFilePath)
            if (ZipUtils.zipFiles(paths, zipFilePath)) {
                val putUrl = "$rootWebDavUrl$backupFileName"
                WebDav(putUrl, it).upload(zipFilePath)
            }
        }
    }

    suspend fun exportWebDav(byteArray: ByteArray, fileName: String) {
        if (!NetworkUtils.isAvailable()) return
        try {
            authorization?.let {
                // 如果导出的本地文件存在,开始上传
                val putUrl = exportsWebDavUrl + fileName
                WebDav(putUrl, it).upload(byteArray, "text/plain")
            }
        } catch (e: Exception) {
            val msg = "WebDav导出\n${e.localizedMessage}"
            AppLog.put(msg, e)
            appCtx.toastOnUi(msg)
        }
    }

    suspend fun exportWebDav(uri: Uri, fileName: String) {
        if (!NetworkUtils.isAvailable()) return
        try {
            authorization?.let {
                // 如果导出的本地文件存在,开始上传
                val putUrl = exportsWebDavUrl + fileName
                WebDav(putUrl, it).upload(uri, "text/plain")
            }
        } catch (e: Exception) {
            val msg = "WebDav导出\n${e.localizedMessage}"
            AppLog.put(msg, e)
            appCtx.toastOnUi(msg)
        }
    }

    fun uploadBookProgress(book: Book) {
        val authorization = authorization ?: return
        if (!AppConfig.syncBookProgress) return
        if (!NetworkUtils.isAvailable()) return
        Coroutine.async {
            val bookProgress = BookProgress(book)
            val json = GSON.toJson(bookProgress)
            val url = getProgressUrl(book.name, book.author)
            WebDav(url, authorization).upload(json.toByteArray(), "application/json")
        }.onError {
            AppLog.put("上传进度失败\n${it.localizedMessage}", it)
        }
    }

    fun uploadBookProgress(bookProgress: BookProgress) {
        val authorization = authorization ?: return
        if (!AppConfig.syncBookProgress) return
        if (!NetworkUtils.isAvailable()) return
        Coroutine.async {
            val json = GSON.toJson(bookProgress)
            val url = getProgressUrl(bookProgress.name, bookProgress.author)
            WebDav(url, authorization).upload(json.toByteArray(), "application/json")
        }.onError {
            AppLog.put("上传进度失败\n${it.localizedMessage}", it)
        }
    }

    private fun getProgressUrl(name: String, author: String): String {
        return bookProgressUrl + UrlUtil.replaceReservedChar("${name}_${author}") + ".json"
    }

    /**
     * 获取书籍进度
     */
    suspend fun getBookProgress(book: Book): BookProgress? {
        authorization?.let {
            val url = getProgressUrl(book.name, book.author)
            kotlin.runCatching {
                WebDav(url, it).download().let { byteArray ->
                    val json = String(byteArray)
                    if (json.isJson()) {
                        return GSON.fromJsonObject<BookProgress>(json).getOrNull()
                    }
                }
            }
        }
        return null
    }

    suspend fun downloadAllBookProgress() {
        authorization ?: return
        if (!NetworkUtils.isAvailable()) return
        appDb.bookDao.all.forEach { book ->
            getBookProgress(book)?.let { bookProgress ->
                if (bookProgress.durChapterIndex > book.durChapterIndex
                    || (bookProgress.durChapterIndex == book.durChapterIndex
                            && bookProgress.durChapterPos > book.durChapterPos)
                ) {
                    book.durChapterIndex = bookProgress.durChapterIndex
                    book.durChapterPos = bookProgress.durChapterPos
                    book.durChapterTitle = bookProgress.durChapterTitle
                    book.durChapterTime = bookProgress.durChapterTime
                    appDb.bookDao.update(book)
                }
            }
        }
    }

}