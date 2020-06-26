package io.legado.app.help.storage

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.documentfile.provider.DocumentFile
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.ParseContext
import io.legado.app.App
import io.legado.app.BuildConfig
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.*
import io.legado.app.help.AppConfig
import io.legado.app.help.LauncherIconHelp
import io.legado.app.help.ReadBookConfig
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.page.ChapterProvider
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import org.jetbrains.anko.defaultSharedPreferences
import java.io.File

object Restore {
    val jsonPath: ParseContext by lazy {
        JsonPath.using(
            Configuration.builder()
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build()
        )
    }

    suspend fun restore(context: Context, path: String) {
        withContext(IO) {
            if (path.isContentPath()) {
                DocumentFile.fromTreeUri(context, Uri.parse(path))?.listFiles()?.forEach { doc ->
                    for (fileName in Backup.backupFileNames) {
                        if (doc.name == fileName) {
                            DocumentUtils.readText(context, doc.uri)?.let {
                                FileUtils.createFileIfNotExist(Backup.backupPath + File.separator + fileName)
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
                                    FileUtils.createFileIfNotExist(Backup.backupPath + File.separator + fileName),
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
                App.db.bookDao().insert(*it.toTypedArray())
            }
            fileToListT<BookGroup>(path, "bookGroup.json")?.let {
                App.db.bookGroupDao().insert(*it.toTypedArray())
            }
            fileToListT<BookSource>(path, "bookSource.json")?.let {
                App.db.bookSourceDao().insert(*it.toTypedArray())
            }
            fileToListT<RssSource>(path, "rssSource.json")?.let {
                App.db.rssSourceDao().insert(*it.toTypedArray())
            }
            fileToListT<RssStar>(path, "rssStar.json")?.let {
                App.db.rssStarDao().insert(*it.toTypedArray())
            }
            fileToListT<ReplaceRule>(path, "replaceRule.json")?.let {
                App.db.replaceRuleDao().insert(*it.toTypedArray())
            }
        }
    }

    suspend fun restoreConfig(path: String = Backup.backupPath) {
        withContext(IO) {
            try {
                val file =
                    FileUtils.createFileIfNotExist(path + File.separator + ReadBookConfig.readConfigFileName)
                val configFile =
                    FileUtils.getFile(App.INSTANCE.filesDir, ReadBookConfig.readConfigFileName)
                if (file.exists()) {
                    file.copyTo(configFile, true)
                    ReadBookConfig.upConfig()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Preferences.getSharedPreferences(App.INSTANCE, path, "config")?.all?.map {
                val edit = App.INSTANCE.defaultSharedPreferences.edit()
                when (val value = it.value) {
                    is Int -> edit.putInt(it.key, value)
                    is Boolean -> edit.putBoolean(it.key, value)
                    is Long -> edit.putLong(it.key, value)
                    is Float -> edit.putFloat(it.key, value)
                    is String -> edit.putString(it.key, value)
                    else -> Unit
                }
                edit.putInt(PreferKey.versionCode, App.INSTANCE.versionCode)
                edit.apply()
            }
            ReadBookConfig.apply {
                styleSelect = App.INSTANCE.getPrefInt(PreferKey.readStyleSelect)
                shareLayout = App.INSTANCE.getPrefBoolean(PreferKey.shareLayout)
                pageAnim = App.INSTANCE.getPrefInt(PreferKey.pageAnim)
                hideStatusBar = App.INSTANCE.getPrefBoolean(PreferKey.hideStatusBar)
                hideNavigationBar = App.INSTANCE.getPrefBoolean(PreferKey.hideNavigationBar)
                bodyIndentCount = App.INSTANCE.getPrefInt(PreferKey.bodyIndent, 2)
                autoReadSpeed = App.INSTANCE.getPrefInt(PreferKey.autoReadSpeed, 46)
            }
            ChapterProvider.upStyle()
            ReadBook.loadContent(resetPageOffset = false)
        }
        withContext(Main) {
            if (AppConfig.isNightTheme && AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES) {
                App.INSTANCE.applyDayNight()
            } else if (!AppConfig.isNightTheme && AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                App.INSTANCE.applyDayNight()
            } else {
                postEvent(EventBus.RECREATE, "true")
            }
            if (!BuildConfig.DEBUG) {
                LauncherIconHelp.changeIcon(App.INSTANCE.getPrefString(PreferKey.launcherIcon))
            }
        }
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