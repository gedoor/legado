package io.legado.app.help.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.ParseContext
import io.legado.app.App
import io.legado.app.constant.AppConst
import io.legado.app.data.entities.*
import io.legado.app.help.ReadBookConfig
import io.legado.app.utils.*
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
                            FileUtils.getFile(Backup.backupPath + File.separator + fileName)
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
            try {
                val file = FileUtils.getFile(path + File.separator + "bookshelf.json")
                val json = file.readText()
                GSON.fromJsonArray<Book>(json)?.let {
                    App.db.bookDao().insert(*it.toTypedArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                val file = FileUtils.getFile(path + File.separator + "bookGroup.json")
                val json = file.readText()
                GSON.fromJsonArray<BookGroup>(json)?.let {
                    App.db.bookGroupDao().insert(*it.toTypedArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                val file = FileUtils.getFile(path + File.separator + "bookSource.json")
                val json = file.readText()
                GSON.fromJsonArray<BookSource>(json)?.let {
                    App.db.bookSourceDao().insert(*it.toTypedArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                val file = FileUtils.getFile(path + File.separator + "rssSource.json")
                val json = file.readText()
                GSON.fromJsonArray<RssSource>(json)?.let {
                    App.db.rssSourceDao().insert(*it.toTypedArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                val file = FileUtils.getFile(path + File.separator + "replaceRule.json")
                val json = file.readText()
                GSON.fromJsonArray<ReplaceRule>(json)?.let {
                    App.db.replaceRuleDao().insert(*it.toTypedArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                val file =
                    FileUtils.getFile(path + File.separator + ReadBookConfig.readConfigFileName)
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
                edit.commit()
            }
        }
    }

    fun importYueDuData(context: Context) {
        GlobalScope.launch(IO) {
            try {// 导入书架
                val shelfFile =
                    FileUtils.getFile(Backup.defaultPath + File.separator + "myBookShelf.json")
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
                    FileUtils.getFile(Backup.defaultPath + File.separator + "myBookSource.json")
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
                    FileUtils.getFile(Backup.defaultPath + File.separator + "myBookReplaceRule.json")
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
        val books = mutableListOf<Book>()
        val items: List<Map<String, Any>> = jsonPath.parse(json).read("$")
        val existingBooks = App.db.bookDao().allBookUrls.toSet()
        for (item in items) {
            val jsonItem = jsonPath.parse(item)
            val book = Book()
            book.bookUrl = jsonItem.readString("$.noteUrl") ?: ""
            if (book.bookUrl.isBlank()) continue
            book.name = jsonItem.readString("$.bookInfoBean.name") ?: ""
            if (book.bookUrl in existingBooks) {
                Log.d(AppConst.APP_TAG, "Found existing book: ${book.name}")
                continue
            }
            book.origin = jsonItem.readString("$.tag") ?: ""
            book.originName = jsonItem.readString("$.bookInfoBean.origin") ?: ""
            book.author = jsonItem.readString("$.bookInfoBean.author") ?: ""
            book.type =
                if (jsonItem.readString("$.bookInfoBean.bookSourceType") == "AUDIO") 1 else 0
            book.tocUrl = jsonItem.readString("$.bookInfoBean.chapterUrl") ?: book.bookUrl
            book.coverUrl = jsonItem.readString("$.bookInfoBean.coverUrl")
            book.customCoverUrl = jsonItem.readString("$.customCoverPath")
            book.lastCheckTime = jsonItem.readLong("$.bookInfoBean.finalRefreshData") ?: 0
            book.canUpdate = jsonItem.readBool("$.allowUpdate") == true
            book.totalChapterNum = jsonItem.readInt("$.chapterListSize") ?: 0
            book.durChapterIndex = jsonItem.readInt("$.durChapter") ?: 0
            book.durChapterTitle = jsonItem.readString("$.durChapterName")
            book.durChapterPos = jsonItem.readInt("$.durChapterPage") ?: 0
            book.durChapterTime = jsonItem.readLong("$.finalDate") ?: 0
            book.group = jsonItem.readInt("$.group") ?: 0
            book.intro = jsonItem.readString("$.bookInfoBean.introduce")
            book.latestChapterTitle = jsonItem.readString("$.lastChapterName")
            book.lastCheckCount = jsonItem.readInt("$.newChapters") ?: 0
            book.order = jsonItem.readInt("$.serialNumber") ?: 0
            book.useReplaceRule = jsonItem.readBool("$.useReplaceRule") == true
            book.variable = jsonItem.readString("$.variable")
            books.add(book)
        }
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