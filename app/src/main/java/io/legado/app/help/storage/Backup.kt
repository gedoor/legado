package io.legado.app.help.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.constant.AppLog
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.help.AppWebDav
import io.legado.app.help.DirectLinkUpload
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.LocalConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.help.config.ThemeConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.utils.*
import io.legado.app.utils.compress.ZipUtils
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 备份
 */
object Backup {

    val backupPath: String by lazy {
        appCtx.filesDir.getFile("backup").createFolderIfNotExist().absolutePath
    }
    val zipFilePath = "${appCtx.externalFiles.absolutePath}${File.separator}backup.zip"

    private val backupFileNames by lazy {
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
            "txtTocRule.json",
            "httpTTS.json",
            "keyboardAssists.json",
            "dictRule.json",
            "servers.json",
            DirectLinkUpload.ruleFileName,
            ReadBookConfig.configFileName,
            ReadBookConfig.shareConfigFileName,
            ThemeConfig.configFileName,
            "config.xml"
        )
    }

    private fun getNowZipFileName(): String {
        val backupDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(System.currentTimeMillis()))
        val deviceName = AppConfig.webDavDeviceName
        return if (deviceName?.isNotBlank() == true) {
            "backup${backupDate}-${deviceName}.zip"
        } else {
            "backup${backupDate}.zip"
        }
    }

    fun autoBack(context: Context) {
        val lastBackup = LocalConfig.lastBackup
        if (lastBackup + TimeUnit.DAYS.toMillis(1) < System.currentTimeMillis()) {
            Coroutine.async {
                val backupZipFileName = getNowZipFileName()
                if (!AppWebDav.hasBackUp(backupZipFileName)) {
                    backup(context, context.getPrefString(PreferKey.backupPath))
                } else {
                    LocalConfig.lastBackup = System.currentTimeMillis()
                }
            }.onError {
                AppLog.put("自动备份失败\n${it.localizedMessage}")
            }
        }
    }

    suspend fun backup(context: Context, path: String?) {
        LocalConfig.lastBackup = System.currentTimeMillis()
        withContext(IO) {
            FileUtils.delete(backupPath)
            writeListToJson(appDb.bookDao.all, "bookshelf.json", backupPath)
            writeListToJson(appDb.bookmarkDao.all, "bookmark.json", backupPath)
            writeListToJson(appDb.bookGroupDao.all, "bookGroup.json", backupPath)
            writeListToJson(appDb.bookSourceDao.all, "bookSource.json", backupPath)
            writeListToJson(appDb.rssSourceDao.all, "rssSources.json", backupPath)
            writeListToJson(appDb.rssStarDao.all, "rssStar.json", backupPath)
            ensureActive()
            writeListToJson(appDb.replaceRuleDao.all, "replaceRule.json", backupPath)
            writeListToJson(appDb.readRecordDao.all, "readRecord.json", backupPath)
            writeListToJson(appDb.searchKeywordDao.all, "searchHistory.json", backupPath)
            writeListToJson(appDb.ruleSubDao.all, "sourceSub.json", backupPath)
            writeListToJson(appDb.txtTocRuleDao.all, "txtTocRule.json", backupPath)
            writeListToJson(appDb.httpTTSDao.all, "httpTTS.json", backupPath)
            writeListToJson(appDb.keyboardAssistsDao.all, "keyboardAssists.json", backupPath)
            writeListToJson(appDb.dictRuleDao.all, "dictRule.json", backupPath)
            writeListToJson(appDb.serverDao.all, "servers.json", backupPath)
            ensureActive()
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
            DirectLinkUpload.getConfig()?.let {
                FileUtils.createFileIfNotExist(backupPath + File.separator + DirectLinkUpload.ruleFileName)
                    .writeText(GSON.toJson(it))
            }
            ensureActive()
            appCtx.getSharedPreferences(backupPath, "config")?.let { sp ->
                val edit = sp.edit()
                appCtx.defaultSharedPreferences.all.forEach { (key, value) ->
                    if (BackupConfig.keyIsNotIgnore(key)) {
                        when (value) {
                            is Int -> edit.putInt(key, value)
                            is Boolean -> edit.putBoolean(key, value)
                            is Long -> edit.putLong(key, value)
                            is Float -> edit.putFloat(key, value)
                            is String -> edit.putString(key, value)
                        }
                    }
                }
                edit.commit()
            }
            ensureActive()
            val zipFileName = getNowZipFileName()
            val paths = arrayListOf(*backupFileNames)
            for (i in 0 until paths.size) {
                paths[i] = backupPath + File.separator + paths[i]
            }
            FileUtils.delete(zipFilePath)
            if (ZipUtils.zipFiles(paths, zipFilePath)) {
                when {
                    path.isNullOrBlank() -> {
                        copyBackup(context.getExternalFilesDir(null)!!, "backup.zip")
                    }
                    path.isContentScheme() -> {
                        copyBackup(context, Uri.parse(path), "backup.zip")
                    }
                    else -> {
                        copyBackup(File(path), "backup.zip")
                    }
                }
                AppWebDav.backUpWebDav(zipFileName)
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

    @Throws(Exception::class)
    @Suppress("SameParameterValue")
    private fun copyBackup(context: Context, uri: Uri, fileName: String) {
        DocumentFile.fromTreeUri(context, uri)?.let { treeDoc ->
            treeDoc.findFile(fileName)?.delete()
            treeDoc.createFile("", fileName)?.openOutputStream()?.use { outputS ->
                FileInputStream(File(zipFilePath)).use { inputS ->
                    inputS.copyTo(outputS)
                }
            }
        }
    }

    @Throws(Exception::class)
    @Suppress("SameParameterValue")
    private fun copyBackup(rootFile: File, fileName: String) {
        FileInputStream(File(zipFilePath)).use { inputS ->
            val file = FileUtils.createFileIfNotExist(rootFile, fileName)
            FileOutputStream(file).use { outputS ->
                inputS.copyTo(outputS)
            }
        }
    }
}
