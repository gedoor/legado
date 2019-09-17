package io.legado.app.help.storage

import androidx.appcompat.app.AppCompatActivity
import io.legado.app.App
import io.legado.app.R
import io.legado.app.help.FileHelp
import io.legado.app.utils.FileUtils
import io.legado.app.utils.GSON
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.File

object Backup {

    val defaultPath by lazy {
        FileUtils.getSdCardPath() + File.separator + "YueDu" + File.separator + "legadoBackUp"
    }

    fun backup() {
        doAsync {
            val path = defaultPath
            backupBookshelf(path)
            backupBookSource(path)
            backupRssSource(path)
            backupReplaceRule(path)
            WebDavHelp.backUpWebDav()
            uiThread {
                App.INSTANCE.toast(R.string.backup_success)
            }
        }
    }

    fun autoBackup(activity: AppCompatActivity) {
        doAsync {
            val path = defaultPath
            backupBookshelf(path)
            backupBookSource(path)
            backupRssSource(path)
            backupReplaceRule(path)
            WebDavHelp.backUpWebDav()
        }
    }

    private fun backupBookshelf(path: String) {
        val json = GSON.toJson(App.db.bookDao().allBooks)
        val file = FileHelp.getFile(path + File.separator + "bookshelf.json")
        file.writeText(json)
    }

    private fun backupBookSource(path: String) {
        val json = GSON.toJson(App.db.bookSourceDao().all)
        val file = FileHelp.getFile(path + File.separator + "bookSource.json")
        file.writeText(json)
    }

    private fun backupRssSource(path: String) {
        val json = GSON.toJson(App.db.rssSourceDao().all)
        val file = FileHelp.getFile(path + File.separator + "rssSource.json")
        file.writeText(json)
    }

    private fun backupReplaceRule(path: String) {
        val json = GSON.toJson(App.db.replaceRuleDao().all)
        val file = FileHelp.getFile(path + File.separator + "replaceRule.json")
        file.writeText(json)
    }
}