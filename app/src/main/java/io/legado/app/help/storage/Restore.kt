package io.legado.app.help.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.ParseContext
import io.legado.app.App
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.*
import io.legado.app.help.LauncherIconHelp
import io.legado.app.help.ReadBookConfig
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.page.ChapterProvider
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
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
        restore(Backup.backupPath)
    }

    suspend fun restore(path: String) {
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
            try {
                val file =
                    FileUtils.createFileIfNotExist(path + File.separator + ReadBookConfig.readConfigFileName)
                val configFile =
                    File(App.INSTANCE.filesDir.absolutePath + File.separator + ReadBookConfig.readConfigFileName)
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
            }
            ChapterProvider.upStyle()
            ReadBook.loadContent()
        }
        LauncherIconHelp.changeIcon(App.INSTANCE.getPrefString(PreferKey.launcherIcon))
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