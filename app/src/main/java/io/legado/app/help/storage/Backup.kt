package io.legado.app.help.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
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
            "rssStar.json",
            "replaceRule.json",
            ReadBookConfig.readConfigFileName,
            "config.xml"
        )
    }

    suspend fun backup(context: Context, uri: Uri?) {
        withContext(IO) {
            writeListToJson(App.db.bookDao().all, "bookshelf.json", backupPath)
            writeListToJson(App.db.bookGroupDao().all, "bookGroup.json", backupPath)
            writeListToJson(App.db.bookSourceDao().all, "bookSource.json", backupPath)
            writeListToJson(App.db.rssSourceDao().all, "rssSource.json", backupPath)
            writeListToJson(App.db.rssStarDao().all, "rssStar.json", backupPath)
            writeListToJson(App.db.replaceRuleDao().all, "replaceRule.json", backupPath)
            GSON.toJson(ReadBookConfig.configList)?.let {
                FileUtils.createFileIfNotExist(backupPath + File.separator + ReadBookConfig.readConfigFileName)
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

    private fun writeListToJson(list: List<Any>, fileName: String, path: String) {
        if (list.isNotEmpty()) {
            val json = GSON.toJson(list)
            FileUtils.createFileIfNotExist(path + File.separator + fileName).writeText(json)
        }
    }

    private fun copyBackup(context: Context, uri: Uri) {
        DocumentFile.fromTreeUri(context, uri)?.let { treeDoc ->
            for (fileName in backupFileNames) {
                val doc = treeDoc.findFile(fileName) ?: treeDoc.createFile("", fileName)
                doc?.let {
                    DocumentUtils.writeText(
                        context,
                        FileUtils.createFileIfNotExist(backupPath + File.separator + fileName).readText(),
                        doc.uri
                    )
                }
            }
        }
    }

    private fun copyBackup() {
        try {
            for (fileName in backupFileNames) {
                FileUtils.createFileIfNotExist(backupPath + File.separator + "bookshelf.json")
                    .copyTo(
                        FileUtils.createFileIfNotExist(legadoPath + File.separator + "bookshelf.json"),
                        true
                    )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}