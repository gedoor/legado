package io.legado.app.help.storage

import androidx.appcompat.app.AppCompatActivity
import io.legado.app.App
import io.legado.app.R
import io.legado.app.help.FileHelp
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.utils.FileUtils
import io.legado.app.utils.GSON
import java.io.File

object Backup {

    fun backup(activity: AppCompatActivity) {
        PermissionsCompat.Builder(activity)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                val path =
                    FileUtils.getSdCardPath() + File.separator + "YueDu" + File.separator + "legadoBackUp"
                backupBookshelf(path)
                backupBookSource(path)
                backupRssSource(path)
                backupReplaceRule(path)
            }
            .request()
    }

    fun autoBackup(activity: AppCompatActivity) {
        PermissionsCompat.Builder(activity)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                val path =
                    FileUtils.getSdCardPath() + File.separator + "YueDu" + File.separator + "legadoBackUp"
                backupBookshelf(path)
                backupBookSource(path)
                backupRssSource(path)
                backupReplaceRule(path)
            }
            .request()
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