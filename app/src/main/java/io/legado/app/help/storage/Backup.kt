package io.legado.app.help.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.constant.PreferKey
import io.legado.app.help.DefaultData
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.ThemeConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.jetbrains.anko.defaultSharedPreferences
import java.io.File
import java.util.concurrent.TimeUnit


object Backup {

    val backupPath: String by lazy {
        FileUtils.getFile(App.INSTANCE.filesDir, "backup").absolutePath
    }

    val backupFileNames by lazy {
        arrayOf(
            "bookshelf.json",
            "bookmark.json",
            "bookGroup.json",
            "bookSource.json",
            "rssSource.json",
            "rssStar.json",
            "replaceRule.json",
            "readRecord.json",
            "searchHistory.json",
            "sourceSub.json",
            DefaultData.txtTocRuleFileName,
            DefaultData.httpTtsFileName,
            ReadBookConfig.configFileName,
            ReadBookConfig.shareConfigFileName,
            ThemeConfig.configFileName,
            "config.xml"
        )
    }

    fun autoBack(context: Context) {
        val lastBackup = context.getPrefLong(PreferKey.lastBackup)
        if (lastBackup + TimeUnit.DAYS.toMillis(1) < System.currentTimeMillis()) {
            Coroutine.async {
                backup(context, context.getPrefString(PreferKey.backupPath) ?: "", true)
            }
        }
    }

    suspend fun backup(context: Context, path: String, isAuto: Boolean = false) {
        context.putPrefLong(PreferKey.lastBackup, System.currentTimeMillis())
        withContext(IO) {
            FileUtils.deleteFile(backupPath)
            writeListToJson(App.db.bookDao.all, "bookshelf.json", backupPath)
            writeListToJson(App.db.bookmarkDao.all, "bookmark.json", backupPath)
            writeListToJson(App.db.bookGroupDao.all, "bookGroup.json", backupPath)
            writeListToJson(App.db.bookSourceDao.all, "bookSource.json", backupPath)
            writeListToJson(App.db.rssSourceDao.all, "rssSource.json", backupPath)
            writeListToJson(App.db.rssStarDao.all, "rssStar.json", backupPath)
            writeListToJson(App.db.replaceRuleDao.all, "replaceRule.json", backupPath)
            writeListToJson(App.db.readRecordDao.all, "readRecord.json", backupPath)
            writeListToJson(App.db.searchKeywordDao.all, "searchHistory.json", backupPath)
            writeListToJson(App.db.ruleSubDao.all, "sourceSub.json", backupPath)
            writeListToJson(App.db.txtTocRule.all, DefaultData.txtTocRuleFileName, backupPath)
            writeListToJson(App.db.httpTTSDao.all, DefaultData.httpTtsFileName, backupPath)
            GSON.toJson(ReadBookConfig.configList).let {
                FileUtils.createFileIfNotExist(backupPath + File.separator + ReadBookConfig.configFileName)
                    .writeText(it)
            }
            GSON.toJson(ReadBookConfig.shareConfig).let {
                FileUtils.createFileIfNotExist(backupPath + File.separator + ReadBookConfig.shareConfigFileName)
            }
            GSON.toJson(ThemeConfig.configList).let {
                FileUtils.createFileIfNotExist(backupPath + File.separator + ThemeConfig.configFileName)
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
            BookWebDav.backUpWebDav(backupPath)
            if (path.isContentScheme()) {
                copyBackup(context, Uri.parse(path), isAuto)
            } else {
                if (path.isEmpty()) {
                    copyBackup(context.getExternalFilesDir(null)!!, false)
                } else {
                    copyBackup(File(path), isAuto)
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
    private fun copyBackup(context: Context, uri: Uri, isAuto: Boolean) {
        DocumentFile.fromTreeUri(context, uri)?.let { treeDoc ->
            for (fileName in backupFileNames) {
                val file = File(backupPath + File.separator + fileName)
                if (file.exists()) {
                    if (isAuto) {
                        treeDoc.findFile("auto")?.findFile(fileName)?.delete()
                        DocumentUtils.createFileIfNotExist(
                            treeDoc,
                            fileName,
                            subDirs = arrayOf("auto")
                        )?.writeBytes(context, file.readBytes())
                    } else {
                        treeDoc.findFile(fileName)?.delete()
                        treeDoc.createFile("", fileName)
                            ?.writeBytes(context, file.readBytes())
                    }
                }
            }
        }
    }

    @Throws(java.lang.Exception::class)
    private fun copyBackup(rootFile: File, isAuto: Boolean) {
        for (fileName in backupFileNames) {
            val file = File(backupPath + File.separator + fileName)
            if (file.exists()) {
                file.copyTo(
                    if (isAuto) {
                        FileUtils.createFileIfNotExist(rootFile, "auto", fileName)
                    } else {
                        FileUtils.createFileIfNotExist(rootFile, fileName)
                    }, true
                )
            }
        }
    }
}