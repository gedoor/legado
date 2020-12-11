package io.legado.app.help.storage

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookProgress
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.webdav.HttpAuth
import io.legado.app.lib.webdav.WebDav
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object BookWebDav {
    private const val defaultWebDavUrl = "https://dav.jianguoyun.com/dav/"
    private val bookProgressUrl = "${rootWebDavUrl}bookProgress/"
    private val zipFilePath = "${FileUtils.getCachePath()}${File.separator}backup.zip"

    private val rootWebDavUrl: String
        get() {
            var url = App.INSTANCE.getPrefString(PreferKey.webDavUrl)
            if (url.isNullOrEmpty()) {
                url = defaultWebDavUrl
            }
            if (!url.endsWith("/")) url = "${url}/"
            if (App.INSTANCE.getPrefBoolean(PreferKey.webDavCreateDir, true)) {
                url = "${url}legado/"
            }
            return url
        }

    suspend fun initWebDav(): Boolean {
        val account = App.INSTANCE.getPrefString(PreferKey.webDavAccount)
        val password = App.INSTANCE.getPrefString(PreferKey.webDavPassword)
        if (!account.isNullOrBlank() && !password.isNullOrBlank()) {
            HttpAuth.auth = HttpAuth.Auth(account, password)
            WebDav(rootWebDavUrl).makeAsDir()
            WebDav(bookProgressUrl).makeAsDir()
            return true
        }
        return false
    }

    @Throws(Exception::class)
    private suspend fun getWebDavFileNames(): ArrayList<String> {
        val url = rootWebDavUrl
        val names = arrayListOf<String>()
        if (initWebDav()) {
            var files = WebDav(url).listFiles()
            files = files.reversed()
            files.forEach {
                val name = it.displayName
                if (name?.startsWith("backup") == true) {
                    names.add(name)
                }
            }
        }
        return names
    }

    suspend fun showRestoreDialog(context: Context) {
        val names = withContext(IO) { getWebDavFileNames() }
        if (names.isNotEmpty()) {
            withContext(Main) {
                context.selector(
                    title = context.getString(R.string.select_restore_file),
                    items = names
                ) { _, index ->
                    if (index in 0 until names.size) {
                        Coroutine.async {
                            restoreWebDav(names[index])
                        }.onError {
                            App.INSTANCE.toast("WebDavError:${it.localizedMessage}")
                        }
                    }
                }
            }
        } else {
            throw Exception("Web dav no back up file")
        }
    }

    private suspend fun restoreWebDav(name: String) {
        rootWebDavUrl.let {
            val webDav = WebDav(it + name)
            webDav.downloadTo(zipFilePath, true)
            @Suppress("BlockingMethodInNonBlockingContext")
            ZipUtils.unzipFile(zipFilePath, Backup.backupPath)
            Restore.restoreDatabase()
            Restore.restoreConfig()
        }
    }

    suspend fun backUpWebDav(path: String) {
        try {
            if (initWebDav()) {
                val paths = arrayListOf(*Backup.backupFileNames)
                for (i in 0 until paths.size) {
                    paths[i] = path + File.separator + paths[i]
                }
                FileUtils.deleteFile(zipFilePath)
                if (ZipUtils.zipFiles(paths, zipFilePath)) {
                    val backupDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Date(System.currentTimeMillis()))
                    val putUrl = "${rootWebDavUrl}backup${backupDate}.zip"
                    WebDav(putUrl).upload(zipFilePath)
                }
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                App.INSTANCE.toast("WebDav\n${e.localizedMessage}")
            }
        }
    }

    suspend fun exportWebDav(path: String, fileName: String) {
        try {
            if (initWebDav()) {
                // 默认导出到legado文件夹下exports目录
                val exportsWebDavUrl = rootWebDavUrl + EncoderUtils.escape("exports") + "/"
                // 在legado文件夹创建exports目录,如果不存在的话
                WebDav(exportsWebDavUrl).makeAsDir()
                val file = File("${path}${File.separator}${fileName}")
                // 如果导出的本地文件存在,开始上传
                if (file.exists()) {
                    val putUrl = exportsWebDavUrl + fileName
                    WebDav(putUrl).upload("${path}${File.separator}${fileName}")
                }
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                App.INSTANCE.toast("WebDav导出\n${e.localizedMessage}")
            }
        }
    }

    fun uploadBookProgress(book: Book) {
        Coroutine.async {
            val bookProgress = BookProgress(
                name = book.name,
                author = book.author,
                durChapterIndex = book.durChapterIndex,
                durChapterPos = book.durChapterPos,
                durChapterTime = book.durChapterTime,
                durChapterTitle = book.durChapterTitle
            )
            val json = GSON.toJson(bookProgress)
            val url = getProgressUrl(book)
            if (initWebDav()) {
                WebDav(url).upload(json.toByteArray())
            }
        }
    }

    suspend fun getBookProgress(book: Book): BookProgress? {
        if (initWebDav()) {
            val url = getProgressUrl(book)
            WebDav(url).download()?.let { byteArray ->
                val json = String(byteArray)
                GSON.fromJsonObject<BookProgress>(json)?.let {
                    return it
                }
            }
        }
        return null
    }

    private fun getProgressUrl(book: Book): String {
        return bookProgressUrl + book.name + "_" + book.author + ".json"
    }
}