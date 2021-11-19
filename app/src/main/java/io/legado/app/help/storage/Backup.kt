package io.legado.app.help.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.help.DefaultData
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.ThemeConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit


object Backup {

    val backupPath: String by lazy {
        appCtx.filesDir.getFile("backup").absolutePath
    }

    val backupFileNames by lazy {
        arrayOf(
            "bookshelf.json",
            "bookmark.json",
            "bookGroup.json",
            "bookSource.json",
            "rssSources.json",
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
            }.onError {
                AppLog.put("备份出错\n${it.localizedMessage}", it)
                appCtx.toastOnUi(appCtx.getString(R.string.autobackup_fail, it.localizedMessage))
            }
        }
    }

    suspend fun backup(context: Context, path: String, isAuto: Boolean = false) {
        context.putPrefLong(PreferKey.lastBackup, System.currentTimeMillis())
        withContext(IO) {
            FileUtils.deleteFile(backupPath)
            writeListToJson(appDb.bookDao.all, "bookshelf.json", backupPath)
            writeListToJson(appDb.bookmarkDao.all, "bookmark.json", backupPath)
            writeListToJson(appDb.bookGroupDao.all, "bookGroup.json", backupPath)
            writeListToJson(appDb.bookSourceDao.all, "bookSource.json", backupPath)
            writeListToJson(appDb.rssSourceDao.all, "rssSources.json", backupPath)
            writeListToJson(appDb.rssStarDao.all, "rssStar.json", backupPath)
            writeListToJson(appDb.replaceRuleDao.all, "replaceRule.json", backupPath)
            writeListToJson(appDb.readRecordDao.all, "readRecord.json", backupPath)
            writeListToJson(appDb.searchKeywordDao.all, "searchHistory.json", backupPath)
            writeListToJson(appDb.ruleSubDao.all, "sourceSub.json", backupPath)
            writeListToJson(appDb.txtTocRuleDao.all, DefaultData.txtTocRuleFileName, backupPath)
            writeListToJson(appDb.httpTTSDao.all, DefaultData.httpTtsFileName, backupPath)
            GSON.toJson(ReadBookConfig.configList).let {
                FileUtils.createFileIfNotExist(backupPath + File.separator + ReadBookConfig.configFileName)
                    .writeText(it)
            }
            GSON.toJson(ReadBookConfig.shareConfig).let {
                FileUtils.createFileIfNotExist(backupPath + File.separator + ReadBookConfig.shareConfigFileName)
                    .writeText(it)
            }
            GSON.toJson(ThemeConfig.configList).let {
                FileUtils.createFileIfNotExist(backupPath + File.separator + ThemeConfig.configFileName)
                    .writeText(it)
            }
            Preferences.getSharedPreferences(appCtx, backupPath, "config")?.let { sp ->
                val edit = sp.edit()
                appCtx.defaultSharedPreferences.all.forEach { (key, value) ->
                    when (value) {
                        is Int -> edit.putInt(key, value)
                        is Boolean -> edit.putBoolean(key, value)
                        is Long -> edit.putLong(key, value)
                        is Float -> edit.putFloat(key, value)
                        is String -> edit.putString(key, value)
                    }
                }
                edit.commit()
            }
            AppWebDav.backUpWebDav(backupPath)
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
            val file = FileUtils.createFileIfNotExist(path + File.separator + fileName)
            FileOutputStream(file).use {
                GSON.writeToOutputStream(it, list)
            }
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