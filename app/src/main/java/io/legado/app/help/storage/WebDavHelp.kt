package io.legado.app.help.storage

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.webdav.WebDav
import io.legado.app.lib.webdav.http.HttpAuth
import io.legado.app.utils.FileUtils
import io.legado.app.utils.ZipUtils
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefString
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

object WebDavHelp {
    private const val defaultWebDavUrl = "https://dav.jianguoyun.com/dav/"
    private val zipFilePath = "${FileUtils.getCachePath()}${File.separator}backup.zip"

    val rootWebDavUrl: String
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

    fun initWebDav(): Boolean {
        val account = App.INSTANCE.getPrefString(PreferKey.webDavAccount)
        val password = App.INSTANCE.getPrefString(PreferKey.webDavPassword)
        if (!account.isNullOrBlank() && !password.isNullOrBlank()) {
            HttpAuth.auth = HttpAuth.Auth(account, password)
            WebDav(rootWebDavUrl).makeAsDir()
            return true
        }
        return false
    }

    private fun getWebDavFileNames(): ArrayList<String> {
        val url = rootWebDavUrl
        val names = arrayListOf<String>()
        if (initWebDav()) {
            try {
                var files = WebDav(url).listFiles()
                files = files.reversed()
                for (index: Int in 0 until min(10, files.size)) {
                    files[index].displayName?.let {
                        names.add(it)
                    }
                }
            } catch (e: Exception) {
                return names
            }
        }
        return names
    }

    suspend fun showRestoreDialog(context: Context, restoreSuccess: () -> Unit): Boolean {
        val names = withContext(IO) { getWebDavFileNames() }
        return if (names.isNotEmpty()) {
            withContext(Main) {
                context.selector(
                    title = context.getString(R.string.select_restore_file),
                    items = names
                ) { _, index ->
                    if (index in 0 until names.size) {
                        restoreWebDav(names[index], restoreSuccess)
                    }
                }
            }
            true
        } else {
            false
        }
    }

    private fun restoreWebDav(name: String, success: () -> Unit) {
        Coroutine.async {
            rootWebDavUrl.let {
                if (name == SyncBookProgress.fileName) {
                    SyncBookProgress.downloadBookProgress()
                } else {
                    val webDav = WebDav(it + name)
                    webDav.downloadTo(zipFilePath, true)
                    @Suppress("BlockingMethodInNonBlockingContext")
                    ZipUtils.unzipFile(zipFilePath, Backup.backupPath)
                    Restore.restoreDatabase()
                    Restore.restoreConfig()
                }
            }
        }.onSuccess {
            success.invoke()
        }.onError {
            App.INSTANCE.toast("WebDavError:${it.localizedMessage}")
        }
    }

    fun backUpWebDav(path: String) {
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
}