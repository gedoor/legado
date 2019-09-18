package io.legado.app.help.storage

import android.content.Context
import io.legado.app.App
import io.legado.app.help.FileHelp
import io.legado.app.lib.webdav.WebDav
import io.legado.app.lib.webdav.http.HttpAuth
import io.legado.app.utils.ZipUtils
import io.legado.app.utils.getPrefString
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.selector
import org.jetbrains.anko.uiThread
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

object WebDavHelp {
    private val zipFilePath = FileHelp.getCachePath() + "/backup" + ".zip"

    fun getWebDavUrl(): String? {
        var url = App.INSTANCE.getPrefString("web_dav_url")
        if (url.isNullOrBlank()) return null
        if (!url.endsWith("/")) url += "/"
        return url
    }

    private fun initWebDav(): Boolean {
        val account = App.INSTANCE.getPrefString("web_dav_account")
        val password = App.INSTANCE.getPrefString("web_dav_password")
        if (!account.isNullOrBlank() && !password.isNullOrBlank()) {
            HttpAuth.auth = HttpAuth.Auth(account, password)
            return true
        }
        return false
    }

    private fun getWebDavFileNames(): ArrayList<String> {
        val url = getWebDavUrl()
        val names = arrayListOf<String>()
        if (!url.isNullOrBlank() && initWebDav()) {
            val files = WebDav(url + "legado/").listFiles()
            files.reversed()
            for (index: Int in 0 until min(10, files.size)) {
                files[index].displayName?.let {
                    names.add(it)
                }
            }
        }
        return names
    }

    fun showRestoreDialog(context: Context) {
        doAsync {
            val names = getWebDavFileNames()
            if (names.isNotEmpty()) {
                uiThread {
                    context.selector(title = "选择恢复文件", items = names) { _, index ->
                        if (index in 0 until names.size) {
                            restoreWebDav(names[index])
                        }
                    }
                }
            } else {
                Restore.restore()
            }
        }
    }

    private fun restoreWebDav(name: String) {
        doAsync {
            getWebDavUrl()?.let {
                val file = WebDav(it + "legado/" + name)
                file.downloadTo(zipFilePath, true)
                ZipUtils.unzipFile(zipFilePath, Backup.defaultPath)
                Restore.restore()
            }
        }
    }

    fun backUpWebDav() {
        if (initWebDav()) {
            val paths = arrayListOf<String>()
            paths.add(Backup.defaultPath + File.separator + "bookshelf.json")
            paths.add(Backup.defaultPath + File.separator + "bookSource.json")
            paths.add(Backup.defaultPath + File.separator + "rssSource.json")
            paths.add(Backup.defaultPath + File.separator + "replaceRule.json")
            FileHelp.deleteFile(zipFilePath)
            if (ZipUtils.zipFiles(paths, zipFilePath)) {
                WebDav(getWebDavUrl() + "legado").makeAsDir()
                val putUrl = getWebDavUrl() + "legado/backup" +
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(Date(System.currentTimeMillis())) + ".zip"
                WebDav(putUrl).upload(zipFilePath)
            }
        }
    }
}