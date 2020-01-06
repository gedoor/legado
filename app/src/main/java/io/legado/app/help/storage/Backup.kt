package io.legado.app.help.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.help.FileHelp
import io.legado.app.help.ReadBookConfig
import io.legado.app.utils.DocumentUtils
import io.legado.app.utils.FileUtils
import io.legado.app.utils.GSON
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.jetbrains.anko.defaultSharedPreferences
import java.io.File


object Backup {

    val backupPath = App.INSTANCE.filesDir.absolutePath + File.separator + "backup"

    val defaultPath by lazy {
        FileUtils.getSdCardPath() + File.separator + "YueDu"
    }

    val legadoPath by lazy {
        FileUtils.getSdCardPath() + File.separator + "YueDu3.0"
    }

    val exportPath by lazy {
        legadoPath + File.separator + "Export"
    }

    val backupFileNames by lazy {
        arrayOf(
            "bookshelf.json",
            "bookGroup.json",
            "bookSource.json",
            "rssSource.json",
            "replaceRule.json",
            ReadBookConfig.readConfigFileName,
            "config.xml"
        )
    }

    suspend fun backup(context: Context, uri: Uri?) {
        withContext(IO) {
            App.db.bookDao().allBooks.let {
                if (it.isNotEmpty()) {
                    val json = GSON.toJson(it)
                    FileHelp.getFile(backupPath + File.separator + "bookshelf.json").writeText(json)
                }
            }
            App.db.bookGroupDao().all().let {
                if (it.isNotEmpty()) {
                    val json = GSON.toJson(it)
                    FileHelp.getFile(backupPath + File.separator + "bookGroup.json").writeText(json)
                }
            }
            App.db.bookSourceDao().all.let {
                if (it.isNotEmpty()) {
                    val json = GSON.toJson(it)
                    FileHelp.getFile(backupPath + File.separator + "bookSource.json")
                        .writeText(json)
                }
            }
            App.db.rssSourceDao().all.let {
                if (it.isNotEmpty()) {
                    val json = GSON.toJson(it)
                    FileHelp.getFile(backupPath + File.separator + "rssSource.json").writeText(json)
                }
            }
            App.db.replaceRuleDao().all.let {
                if (it.isNotEmpty()) {
                    val json = GSON.toJson(it)
                    FileHelp.getFile(backupPath + File.separator + "replaceRule.json")
                        .writeText(json)
                }
            }
            GSON.toJson(ReadBookConfig.configList)?.let {
                FileHelp.getFile(backupPath + File.separator + ReadBookConfig.readConfigFileName)
                    .writeText(it)
            }
            Preferences.getSharedPreferences(App.INSTANCE, backupPath, "config")?.let { sp ->
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
            WebDavHelp.backUpWebDav(backupPath)
            if (uri != null) {
                copyBackup(context, uri)
            } else {
                copyBackup()
            }
        }
    }

    private fun copyBackup(context: Context, uri: Uri) {
        DocumentFile.fromTreeUri(context, uri)?.let { treeDoc ->
            for (fileName in backupFileNames) {
                treeDoc.createFile("text/plain", fileName)?.let { doc ->
                    DocumentUtils.writeText(
                        context,
                        FileHelp.getFile(backupPath + File.separator + fileName).readText(),
                        doc.uri
                    )
                }
            }
        }
    }

    private fun copyBackup() {
        for (fileName in backupFileNames) {
            FileHelp.getFile(backupPath + File.separator + "bookshelf.json")
                .copyTo(FileHelp.getFile(legadoPath + File.separator + "bookshelf.json"))
        }
    }
}