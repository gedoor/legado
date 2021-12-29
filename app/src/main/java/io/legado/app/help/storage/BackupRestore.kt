package io.legado.app.help.storage

import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.utils.FileUtils
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import splitties.init.appCtx

abstract class BackupRestore {

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
        PreferKey.threadCount,
        PreferKey.defaultBookTreeUri
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
        PreferKey.themeMode,
        PreferKey.defaultCover,
        PreferKey.defaultCoverDark
    )

    //阅读配置
    private val readPrefKeys = arrayOf(
        PreferKey.readStyleSelect,
        PreferKey.shareLayout,
        PreferKey.hideStatusBar,
        PreferKey.hideNavigationBar,
        PreferKey.autoReadSpeed
    )


    protected fun keyIsNotIgnore(key: String): Boolean {
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

    protected val ignoreReadConfig: Boolean
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

}