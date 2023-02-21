package io.legado.app.help.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.BuildConfig
import io.legado.app.R
import io.legado.app.constant.AppConst.androidId
import io.legado.app.constant.AppLog
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.*
import io.legado.app.help.DirectLinkUpload
import io.legado.app.help.LauncherIconHelp
import io.legado.app.help.book.isLocal
import io.legado.app.help.book.upType
import io.legado.app.help.config.LocalConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.help.config.ThemeConfig
import io.legado.app.model.localBook.LocalBook
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 恢复
 */
object Restore {

    suspend fun restore(context: Context, path: String) {
        kotlin.runCatching {
            if (path.isContentScheme()) {
                DocumentFile.fromTreeUri(context, Uri.parse(path))?.listFiles()?.forEach { doc ->
                    if (Backup.backupFileNames.contains(doc.name)) {
                        context.contentResolver.openInputStream(doc.uri)?.use { inputStream ->
                            val file = File("${Backup.backupPath}${File.separator}${doc.name}")
                            FileOutputStream(file).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }
                }
            } else {
                val dir = File(path)
                for (fileName in Backup.backupFileNames) {
                    val file = dir.getFile(fileName)
                    if (file.exists()) {
                        val target = File("${Backup.backupPath}${File.separator}$fileName")
                        file.copyTo(target, true)
                    }
                }
            }
        }.onFailure {
            AppLog.put("恢复复制文件出错\n${it.localizedMessage}", it)
        }
        restoreDatabase()
        restoreConfig()
        LocalConfig.lastBackup = System.currentTimeMillis()
    }

    suspend fun restoreDatabase(path: String = Backup.backupPath) {
        withContext(IO) {
            fileToListT<Book>(path, "bookshelf.json")?.let {
                it.forEach { book ->
                    book.upType()
                }
                it.filter { book -> book.isLocal }
                    .forEach { book ->
                        book.coverUrl = LocalBook.getCoverPath(book)
                    }
                appDb.bookDao.insert(*it.toTypedArray())
            }
            fileToListT<Bookmark>(path, "bookmark.json")?.let {
                appDb.bookmarkDao.insert(*it.toTypedArray())
            }
            fileToListT<BookGroup>(path, "bookGroup.json")?.let {
                appDb.bookGroupDao.insert(*it.toTypedArray())
            }
            fileToListT<BookSource>(path, "bookSource.json")?.let {
                appDb.bookSourceDao.insert(*it.toTypedArray())
            } ?: run {
                val bookSourceFile =
                    FileUtils.createFileIfNotExist(path + File.separator + "bookSource.json")
                val json = bookSourceFile.readText()
                ImportOldData.importOldSource(json)
            }
            fileToListT<RssSource>(path, "rssSources.json")?.let {
                appDb.rssSourceDao.insert(*it.toTypedArray())
            }
            fileToListT<RssStar>(path, "rssStar.json")?.let {
                appDb.rssStarDao.insert(*it.toTypedArray())
            }
            fileToListT<ReplaceRule>(path, "replaceRule.json")?.let {
                appDb.replaceRuleDao.insert(*it.toTypedArray())
            }
            fileToListT<SearchKeyword>(path, "searchHistory.json")?.let {
                appDb.searchKeywordDao.insert(*it.toTypedArray())
            }
            fileToListT<RuleSub>(path, "sourceSub.json")?.let {
                appDb.ruleSubDao.insert(*it.toTypedArray())
            }
            fileToListT<TxtTocRule>(path, "txtTocRule.json")?.let {
                appDb.txtTocRuleDao.insert(*it.toTypedArray())
            }
            fileToListT<HttpTTS>(path, "httpTTS.json")?.let {
                appDb.httpTTSDao.insert(*it.toTypedArray())
            }
            fileToListT<ReadRecord>(path, "readRecord.json")?.let {
                it.forEach { readRecord ->
                    //判断是不是本机记录
                    if (readRecord.deviceId != androidId) {
                        appDb.readRecordDao.insert(readRecord)
                    } else {
                        val time = appDb.readRecordDao
                            .getReadTime(readRecord.deviceId, readRecord.bookName)
                        if (time == null || time < readRecord.readTime) {
                            appDb.readRecordDao.insert(readRecord)
                        }
                    }
                }
            }
        }
    }

    suspend fun restoreConfig(path: String = Backup.backupPath) {
        withContext(IO) {
            try {
                val file =
                    FileUtils.createFileIfNotExist("$path${File.separator}${DirectLinkUpload.ruleFileName}")
                if (file.exists()) {
                    val json = file.readText()
                    ACache.get(cacheDir = false).put(DirectLinkUpload.ruleFileName, json)
                }
            } catch (e: Exception) {
                AppLog.put("直链上传出错\n${e.localizedMessage}", e)
            }
            try {
                val file =
                    FileUtils.createFileIfNotExist("$path${File.separator}${ThemeConfig.configFileName}")
                if (file.exists()) {
                    FileUtils.delete(ThemeConfig.configFilePath)
                    file.copyTo(File(ThemeConfig.configFilePath))
                    ThemeConfig.upConfig()
                }
            } catch (e: Exception) {
                AppLog.put("恢复主题出错\n${e.localizedMessage}", e)
            }
            if (!BackupConfig.ignoreReadConfig) {
                //恢复阅读界面配置
                try {
                    val file =
                        FileUtils.createFileIfNotExist("$path${File.separator}${ReadBookConfig.configFileName}")
                    if (file.exists()) {
                        FileUtils.delete(ReadBookConfig.configFilePath)
                        file.copyTo(File(ReadBookConfig.configFilePath))
                        ReadBookConfig.initConfigs()
                    }
                } catch (e: Exception) {
                    AppLog.put("恢复阅读界面出错\n${e.localizedMessage}", e)
                }
                try {
                    val file =
                        FileUtils.createFileIfNotExist("$path${File.separator}${ReadBookConfig.shareConfigFileName}")
                    if (file.exists()) {
                        FileUtils.delete(ReadBookConfig.shareConfigFilePath)
                        file.copyTo(File(ReadBookConfig.shareConfigFilePath))
                        ReadBookConfig.initShareConfig()
                    }
                } catch (e: Exception) {
                    AppLog.put("恢复阅读界面出错\n${e.localizedMessage}", e)
                }
            }
            appCtx.getSharedPreferences(path, "config")?.all?.let { map ->
                val edit = appCtx.defaultSharedPreferences.edit()
                map.forEach { (key, value) ->
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
                edit.apply()
            }
            ReadBookConfig.apply {
                styleSelect = appCtx.getPrefInt(PreferKey.readStyleSelect)
                shareLayout = appCtx.getPrefBoolean(PreferKey.shareLayout)
                hideStatusBar = appCtx.getPrefBoolean(PreferKey.hideStatusBar)
                hideNavigationBar = appCtx.getPrefBoolean(PreferKey.hideNavigationBar)
                autoReadSpeed = appCtx.getPrefInt(PreferKey.autoReadSpeed, 46)
            }
        }
        appCtx.toastOnUi(R.string.restore_success)
        withContext(Main) {
            delay(100)
            if (!BuildConfig.DEBUG) {
                LauncherIconHelp.changeIcon(appCtx.getPrefString(PreferKey.launcherIcon))
            }
            postEvent(EventBus.RECREATE, "")
        }
    }

    private inline fun <reified T> fileToListT(path: String, fileName: String): List<T>? {
        try {
            val file = FileUtils.createFileIfNotExist(path + File.separator + fileName)
            FileInputStream(file).use {
                return GSON.fromJsonArray<T>(it).getOrThrow()
            }
        } catch (e: Exception) {
            AppLog.put("$fileName\n读取解析出错\n${e.localizedMessage}", e)
            appCtx.toastOnUi("$fileName\n读取文件出错\n${e.localizedMessage}")
        }
        return null
    }

}