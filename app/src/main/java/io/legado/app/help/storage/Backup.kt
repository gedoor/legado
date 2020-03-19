package io.legado.app.help.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.constant.PreferKey
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.jetbrains.anko.defaultSharedPreferences
import java.io.File
import java.util.concurrent.TimeUnit


object Backup {

    val backupPath: String by lazy {
        FileUtils.getDirFile(App.INSTANCE.filesDir, "backup").absolutePath
    }

    val legadoPath by lazy {
        FileUtils.getSdCardPath() + File.separator + "YueDu3.0"
    }

    val exportPath by lazy {
        legadoPath + File.separator + "Export"
    }

    val backupFileNames by lazy {
        arrayOf(
            "bookshelf.json", "bookGroup.json", "bookSource.json", "rssSource.json",
            "rssStar.json", "replaceRule.json", ReadBookConfig.readConfigFileName, "config.xml"
        )
    }

    fun autoBack(context: Context) {
        val lastBackup = context.getPrefLong(PreferKey.lastBackup)
        if (lastBackup + TimeUnit.DAYS.toMillis(1) < System.currentTimeMillis()) {
            return
        }
        Coroutine.async {
            val backupPath = context.getPrefString(PreferKey.backupPath)
            if (backupPath.isNullOrEmpty()) {
                backup(context)
            } else {
                backup(context, backupPath)
            }
        }
    }

    suspend fun backup(context: Context, path: String = legadoPath) {
        context.putPrefLong(PreferKey.lastBackup, System.currentTimeMillis())
        withContext(IO) {
            synchronized(this@Backup) {
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
                if (path.isContentPath()) {
                    copyBackup(context, Uri.parse(path))
                } else {
                    copyBackup(File(path))
                }
            }
        }
    }

    private fun writeListToJson(list: List<Any>, fileName: String, path: String) {
        if (list.isNotEmpty()) {
            val json = GSON.toJson(list)
            FileUtils.createFileIfNotExist(path + File.separator + fileName).writeText(json)
        }
    }

    @Throws(java.lang.Exception::class)
    private fun copyBackup(context: Context, uri: Uri) {

        DocumentFile.fromTreeUri(context, uri)?.let { treeDoc ->
            for (fileName in backupFileNames) {
                val file = File(backupPath + File.separator + fileName)
                if (file.exists()) {
                    treeDoc.findFile(fileName)?.delete()
                    treeDoc.createFile("", fileName)
                        ?.writeBytes(context, file.readBytes())
                }
            }
        }
    }

    @Throws(java.lang.Exception::class)
    private fun copyBackup(rootFile: File) {
        for (fileName in backupFileNames) {
            val file = File(backupPath + File.separator + fileName)
            if (file.exists()) {
                file.copyTo(
                    FileUtils.createFileIfNotExist(rootFile, fileName),
                    true
                )
            }
        }
    }
}