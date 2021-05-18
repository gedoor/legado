package io.legado.app.help.storage

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.ParseContext
import io.legado.app.BuildConfig
import io.legado.app.R
import io.legado.app.constant.AppConst.androidId
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.*
import io.legado.app.help.DefaultData
import io.legado.app.help.LauncherIconHelp
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.ThemeConfig
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import splitties.systemservices.alarmManager
import java.io.File
import kotlin.system.exitProcess


object Restore {
    private val ignoreConfigPath = FileUtils.getPath(appCtx.filesDir, "restoreIgnore.json")
    val ignoreConfig: HashMap<String, Boolean> by lazy {
        val file = FileUtils.createFileIfNotExist(ignoreConfigPath)
        val json = file.readText()
        GSON.fromJsonObject<HashMap<String, Boolean>>(json) ?: hashMapOf()
    }

    //忽略key
    val ignoreKeys = arrayOf(
        "readConfig",
        PreferKey.themeMode,
        PreferKey.bookshelfLayout,
        PreferKey.showRss,
        PreferKey.threadCount
    )

    //忽略标题
    val ignoreTitle = arrayOf(
        appCtx.getString(R.string.read_config),
        appCtx.getString(R.string.theme_mode),
        appCtx.getString(R.string.bookshelf_layout),
        appCtx.getString(R.string.show_rss),
        appCtx.getString(R.string.thread_count)
    )

    //默认忽略keys
    private val ignorePrefKeys = arrayOf(
        PreferKey.defaultCover
    )
    private val readPrefKeys = arrayOf(
        PreferKey.readStyleSelect,
        PreferKey.shareLayout,
        PreferKey.hideStatusBar,
        PreferKey.hideNavigationBar,
        PreferKey.autoReadSpeed
    )

    val jsonPath: ParseContext by lazy {
        JsonPath.using(
            Configuration.builder()
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build()
        )
    }

    suspend fun restore(context: Context, path: String) {
        withContext(IO) {
            if (path.isContentScheme()) {
                DocumentFile.fromTreeUri(context, Uri.parse(path))?.listFiles()?.forEach { doc ->
                    for (fileName in Backup.backupFileNames) {
                        if (doc.name == fileName) {
                            DocumentUtils.readText(context, doc.uri)?.let {
                                FileUtils.createFileIfNotExist("${Backup.backupPath}${File.separator}$fileName")
                                    .writeText(it)
                            }
                        }
                    }
                }
            } else {
                try {
                    val file = File(path)
                    for (fileName in Backup.backupFileNames) {
                        FileUtils.getFile(file, fileName).let {
                            if (it.exists()) {
                                it.copyTo(
                                    FileUtils.createFileIfNotExist("${Backup.backupPath}${File.separator}$fileName"),
                                    true
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        restoreDatabase()
        restoreConfig()
    }

    suspend fun restoreDatabase(path: String = Backup.backupPath) {
        withContext(IO) {
            fileToListT<Book>(path, "bookshelf.json")?.let {
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
            fileToListT<TxtTocRule>(path, DefaultData.txtTocRuleFileName)?.let {
                appDb.txtTocRuleDao.insert(*it.toTypedArray())
            }
            fileToListT<HttpTTS>(path, DefaultData.httpTtsFileName)?.let {
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
                    FileUtils.createFileIfNotExist("$path${File.separator}${ThemeConfig.configFileName}")
                if (file.exists()) {
                    FileUtils.deleteFile(ThemeConfig.configFilePath)
                    file.copyTo(File(ThemeConfig.configFilePath))
                    ThemeConfig.upConfig()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (!ignoreReadConfig) {
                try {
                    val file =
                        FileUtils.createFileIfNotExist("$path${File.separator}${ReadBookConfig.configFileName}")
                    if (file.exists()) {
                        FileUtils.deleteFile(ReadBookConfig.configFilePath)
                        file.copyTo(File(ReadBookConfig.configFilePath))
                        ReadBookConfig.initConfigs()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    val file =
                        FileUtils.createFileIfNotExist("$path${File.separator}${ReadBookConfig.shareConfigFileName}")
                    if (file.exists()) {
                        FileUtils.deleteFile(ReadBookConfig.shareConfigFilePath)
                        file.copyTo(File(ReadBookConfig.shareConfigFilePath))
                        ReadBookConfig.initShareConfig()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Preferences.getSharedPreferences(appCtx, path, "config")?.all?.let { map ->
                val edit = appCtx.defaultSharedPreferences.edit()
                map.forEach {
                    if (keyIsNotIgnore(it.key)) {
                        when (val value = it.value) {
                            is Int -> edit.putInt(it.key, value)
                            is Boolean -> edit.putBoolean(it.key, value)
                            is Long -> edit.putLong(it.key, value)
                            is Float -> edit.putFloat(it.key, value)
                            is String -> edit.putString(it.key, value)
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
            appCtx.packageManager.getLaunchIntentForPackage(appCtx.packageName)?.let { intent ->
                val restartIntent =
                    PendingIntent.getActivity(appCtx, 0, intent, PendingIntent.FLAG_ONE_SHOT)
                alarmManager[AlarmManager.RTC, System.currentTimeMillis() + 300] = restartIntent
                exitProcess(0)
            }
        }
    }

    private fun keyIsNotIgnore(key: String): Boolean {
        return when {
            ignorePrefKeys.contains(key) -> false
            readPrefKeys.contains(key) && ignoreReadConfig -> false
            PreferKey.themeMode == key && ignoreThemeMode -> false
            PreferKey.bookshelfLayout == key && ignoreBookshelfLayout -> false
            PreferKey.showRss == key && ignoreShowRss -> false
            PreferKey.threadCount == key && ignoreThreadCount -> false
            else -> true
        }
    }

    private val ignoreReadConfig: Boolean
        get() = ignoreConfig["readConfig"] == true
    private val ignoreThemeMode: Boolean
        get() = ignoreConfig[PreferKey.themeMode] == true
    private val ignoreBookshelfLayout: Boolean
        get() = ignoreConfig[PreferKey.bookshelfLayout] == true
    private val ignoreShowRss: Boolean
        get() = ignoreConfig[PreferKey.showRss] == true
    private val ignoreThreadCount: Boolean
        get() = ignoreConfig[PreferKey.threadCount] == true

    fun saveIgnoreConfig() {
        val json = GSON.toJson(ignoreConfig)
        FileUtils.createFileIfNotExist(ignoreConfigPath).writeText(json)
    }

    private inline fun <reified T> fileToListT(path: String, fileName: String): List<T>? {
        try {
            val file = FileUtils.createFileIfNotExist(path + File.separator + fileName)
            val json = file.readText()
            return GSON.fromJsonArray(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

}