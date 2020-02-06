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
import io.legado.app.help.ReadBookConfig
import io.legado.app.utils.DocumentUtils
import io.legado.app.utils.FileUtils
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.toast
import java.io.File

object Restore {
    val jsonPath: ParseContext by lazy {
        JsonPath.using(
            Configuration.builder()
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build()
        )
    }

    suspend fun restore(context: Context, uri: Uri) {
        withContext(IO) {
            DocumentFile.fromTreeUri(context, uri)?.listFiles()?.forEach { doc ->
                for (fileName in Backup.backupFileNames) {
                    if (doc.name == fileName) {
                        DocumentUtils.readText(context, doc.uri)?.let {
                            FileUtils.createFileIfNotExist(Backup.backupPath + File.separator + fileName)
                                .writeText(it)
                        }
                    }
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
                edit.commit()
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

    fun importYueDuData(context: Context) {
        GlobalScope.launch(IO) {
            try {// 导入书架
                val shelfFile =
                    FileUtils.createFileIfNotExist(Backup.defaultPath + File.separator + "myBookShelf.json")
                val json = shelfFile.readText()
                val importCount = importOldBookshelf(json)
                withContext(Main) {
                    context.toast("成功导入书籍${importCount}")
                }
            } catch (e: Exception) {
                withContext(Main) {
                    context.toast("导入书籍失败\n${e.localizedMessage}")
                }
            }

            try {// Book source
                val sourceFile =
                    FileUtils.createFileIfNotExist(Backup.defaultPath + File.separator + "myBookSource.json")
                val json = sourceFile.readText()
                val importCount = importOldSource(json)
                withContext(Main) {
                    context.toast("成功导入书源${importCount}")
                }
            } catch (e: Exception) {
                withContext(Main) {
                    context.toast("导入源失败\n${e.localizedMessage}")
                }
            }

            try {// Replace rules
                val ruleFile =
                    FileUtils.createFileIfNotExist(Backup.defaultPath + File.separator + "myBookReplaceRule.json")
                val json = ruleFile.readText()
                val importCount = importOldReplaceRule(json)
                withContext(Main) {
                    context.toast("成功导入替换规则${importCount}")
                }
            } catch (e: Exception) {
                withContext(Main) {
                    context.toast("导入替换规则失败\n${e.localizedMessage}")
                }
            }
        }
    }

    fun importOldBookshelf(json: String): Int {
        val books = OldBook.toNewBook(json)
        App.db.bookDao().insert(*books.toTypedArray())
        return books.size
    }

    fun importOldSource(json: String): Int {
        val bookSources = mutableListOf<BookSource>()
        val items: List<Map<String, Any>> = jsonPath.parse(json).read("$")
        for (item in items) {
            val jsonItem = jsonPath.parse(item)
            OldRule.jsonToBookSource(jsonItem.jsonString())?.let {
                bookSources.add(it)
            }
        }
        App.db.bookSourceDao().insert(*bookSources.toTypedArray())
        return bookSources.size
    }

    fun importOldReplaceRule(json: String): Int {
        val replaceRules = mutableListOf<ReplaceRule>()
        val items: List<Map<String, Any>> = jsonPath.parse(json).read("$")
        for (item in items) {
            val jsonItem = jsonPath.parse(item)
            OldRule.jsonToReplaceRule(jsonItem.jsonString())?.let {
                replaceRules.add(it)
            }
        }
        App.db.replaceRuleDao().insert(*replaceRules.toTypedArray())
        return replaceRules.size
    }
}