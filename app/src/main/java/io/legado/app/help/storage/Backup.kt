package io.legado.app.help.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.R
import io.legado.app.help.FileHelp
import io.legado.app.help.ReadBookConfig
import io.legado.app.utils.FileUtils
import io.legado.app.utils.GSON
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.File


object Backup {

    val defaultPath by lazy {
        FileUtils.getSdCardPath() + File.separator + "YueDu"
    }

    val legadoPath by lazy {
        FileUtils.getSdCardPath() + File.separator + "YueDu3.0"
    }

    val exportPath by lazy {
        legadoPath + File.separator + "Export"
    }

    fun backup(context: Context, uri: Uri) {
        DocumentFile.fromTreeUri(context, uri)?.listFiles()?.forEach { doc ->
            when (doc.name) {
                "bookshelf.json" -> {

                }
                "bookGroup.json" -> {

                }
                "bookSource.json" -> {

                }
                "rssSource.json" -> {

                }
                "replaceRule.json" -> {

                }
                ReadBookConfig.readConfigFileName -> {

                }
                "config.xml" -> {

                }
            }
        }
    }

    private fun pBackup(path: String = legadoPath) {
        backupBookshelf(path)
        backupBookGroup(path)
        backupBookSource(path)
        backupRssSource(path)
        backupReplaceRule(path)
        backupReadConfig(path)
        backupPreference(path)
        WebDavHelp.backUpWebDav(path)
    }

    fun backup() {
        doAsync {
            pBackup()
            uiThread {
                App.INSTANCE.toast(R.string.backup_success)
            }
        }
    }

    fun autoBackup() {
        doAsync {
            pBackup()
        }
    }

    private fun backupBookshelf(path: String) {
        App.db.bookDao().allBooks.let {
            if (it.isNotEmpty()) {
                val json = GSON.toJson(it)
                val file = FileHelp.getFile(path + File.separator + "bookshelf.json")
                file.writeText(json)
            }
        }
    }

    private fun backupBookGroup(path: String) {
        App.db.bookGroupDao().all().let {
            if (it.isNotEmpty()) {
                val json = GSON.toJson(it)
                val file = FileHelp.getFile(path + File.separator + "bookGroup.json")
                file.writeText(json)
            }
        }
    }

    private fun backupBookSource(path: String) {
        App.db.bookSourceDao().all.let {
            if (it.isNotEmpty()) {
                val json = GSON.toJson(it)
                val file = FileHelp.getFile(path + File.separator + "bookSource.json")
                file.writeText(json)
            }
        }
    }

    private fun backupRssSource(path: String) {
        App.db.rssSourceDao().all.let {
            if (it.isNotEmpty()) {
                val json = GSON.toJson(it)
                val file = FileHelp.getFile(path + File.separator + "rssSource.json")
                file.writeText(json)
            }
        }
    }

    private fun backupReplaceRule(path: String) {
        App.db.replaceRuleDao().all.let {
            if (it.isNotEmpty()) {
                val json = GSON.toJson(it)
                val file = FileHelp.getFile(path + File.separator + "replaceRule.json")
                file.writeText(json)
            }
        }
    }

    private fun backupReadConfig(path: String) {
        GSON.toJson(ReadBookConfig.configList)?.let {
            FileHelp.getFile(path + File.separator + ReadBookConfig.readConfigFileName)
                .writeText(it)
        }
    }

    private fun backupPreference(path: String) {
        Preferences.getSharedPreferences(App.INSTANCE, path, "config")?.let { sp ->
            val edit = sp.edit()
            App.INSTANCE.defaultSharedPreferences.all.map {
                when (val value = it.value) {
                    is Int -> edit.putInt(it.key, value)
                    is Boolean -> edit.putBoolean(it.key, value)
                    is Long -> edit.putLong(it.key, value)
                    is Float -> edit.putFloat(it.key, value)
                    is String -> edit.putString(it.key, value)
                    else -> Unit
                }
            }
            edit.commit()
        }
    }

}