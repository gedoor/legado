package io.legado.app.help.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.ParseContext
import io.legado.app.App
import io.legado.app.BuildConfig
import io.legado.app.R
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.*
import io.legado.app.help.DefaultData
import io.legado.app.help.LauncherIconHelp
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.ThemeConfig
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.toast
import java.io.File

object Restore {
    private val ignoreConfigPath = FileUtils.getPath(App.INSTANCE.filesDir, "restoreIgnore.json")
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
        App.INSTANCE.getString(R.string.read_config),
        App.INSTANCE.getString(R.string.theme_mode),
        App.INSTANCE.getString(R.string.bookshelf_layout),
        App.INSTANCE.getString(R.string.show_rss),
        App.INSTANCE.getString(R.string.thread_count)
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
                App.db.bookDao.insert(*it.toTypedArray())
            }
            fileToListT<Bookmark>(path, "bookmark.json")?.let {
                App.db.bookmarkDao.insert(*it.toTypedArray())
            }
            fileToListT<BookGroup>(path, "bookGroup.json")?.let {
                App.db.bookGroupDao.insert(*it.toTypedArray())
            }
            fileToListT<BookSource>(path, "bookSource.json")?.let {
                App.db.bookSourceDao.insert(*it.toTypedArray())
            }
            fileToListT<RssSource>(path, "rssSource.json")?.let {
                App.db.rssSourceDao.insert(*it.toTypedArray())
            }
            fileToListT<RssStar>(path, "rssStar.json")?.let {
                App.db.rssStarDao.insert(*it.toTypedArray())
            }
            fileToListT<ReplaceRule>(path, "replaceRule.json")?.let {
                App.db.replaceRuleDao.insert(*it.toTypedArray())
            }
            fileToListT<SearchKeyword>(path, "searchHistory.json")?.let {
                App.db.searchKeywordDao.insert(*it.toTypedArray())
            }
            fileToListT<RuleSub>(path, "sourceSub.json")?.let {
                App.db.ruleSubDao.insert(*it.toTypedArray())
            }
            fileToListT<TxtTocRule>(path, DefaultData.txtTocRuleFileName)?.let {
                App.db.txtTocRule.insert(*it.toTypedArray())
            }
            fileToListT<HttpTTS>(path, DefaultData.httpTtsFileName)?.let {
                App.db.httpTTSDao.insert(*it.toTypedArray())
            }
            fileToListT<ReadRecord>(path, "readRecord.json")?.let {
                it.forEach { readRecord ->
                    //判断是不是本机记录
                    if (readRecord.androidId != App.androidId) {
                        App.db.readRecordDao.insert(readRecord)
                    } else {
                        val time = App.db.readRecordDao
                            .getReadTime(readRecord.androidId, readRecord.bookName)
                        if (time == null || time < readRecord.readTime) {
                            App.db.readRecordDao.insert(readRecord)
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
            Preferences.getSharedPreferences(App.INSTANCE, path, "config")?.all?.let { map ->
                val edit = App.INSTANCE.defaultSharedPreferences.edit()
                map.forEach {
                    if (keyIsNotIgnore(it.key)) {
                        when (val value = it.value) {
                            is Int -> edit.putInt(it.key, value)
                            is Boolean -> edit.putBoolean(it.key, value)
                            is Long -> edit.putLong(it.key, value)
                            is Float -> edit.putFloat(it.key, value)
                            is String -> edit.putString(it.key, value)
                            else -> Unit
                        }
                    }
                }
                edit.apply()
            }
            ReadBookConfig.apply {
                styleSelect = App.INSTANCE.getPrefInt(PreferKey.readStyleSelect)
                shareLayout = App.INSTANCE.getPrefBoolean(PreferKey.shareLayout)
                hideStatusBar = App.INSTANCE.getPrefBoolean(PreferKey.hideStatusBar)
                hideNavigationBar = App.INSTANCE.getPrefBoolean(PreferKey.hideNavigationBar)
                autoReadSpeed = App.INSTANCE.getPrefInt(PreferKey.autoReadSpeed, 46)
            }
            ChapterProvider.upStyle()
            ReadBook.loadContent(resetPageOffset = false)
        }
        withContext(Main) {
            App.INSTANCE.toast(R.string.restore_success)
            if (!BuildConfig.DEBUG) {
                LauncherIconHelp.changeIcon(App.INSTANCE.getPrefString(PreferKey.launcherIcon))
            }
            LanguageUtils.setConfiguration(App.INSTANCE)
            App.INSTANCE.applyDayNight()
            postEvent(EventBus.SHOW_RSS, "")
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