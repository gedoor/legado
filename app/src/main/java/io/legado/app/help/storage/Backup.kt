package io.legado.app.help.storage

import io.legado.app.App
import io.legado.app.help.FileHelp
import io.legado.app.utils.GSON
import java.io.File

object Backup {

    fun backup() {

    }

    fun autoBackup() {

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

    private fun backupCssSource(path: String) {
        val json = GSON.toJson(App.db.rssSourceDao().all)
        val file = FileHelp.getFile(path + File.separator + "cssSource.json")
        file.writeText(json)
    }

    private fun backupReplaceRule(path: String) {
        val json = GSON.toJson(App.db.replaceRuleDao().all)
        val file = FileHelp.getFile(path + File.separator + "replaceRule.json")
        file.writeText(json)
    }
}