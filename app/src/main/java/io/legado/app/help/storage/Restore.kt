package io.legado.app.help.storage

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.data.entities.RssSource
import io.legado.app.help.FileHelp
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.utils.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.File

object Restore {
    private val jsonPath = JsonPath.using(
        Configuration.builder()
            .options(Option.SUPPRESS_EXCEPTIONS)
            .build()
    )

    fun restore(activity: AppCompatActivity) {
        PermissionsCompat.Builder(activity)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                val path =
                    FileUtils.getSdCardPath() + File.separator + "YueDu" + File.separator + "legadoBackUp"
                restore(path)
            }
            .request()
    }

    fun restore(path: String) {
        try {
            val file = FileHelp.getFile(path + File.separator + "bookshelf.json")
            val json = file.readText()
            GSON.fromJsonArray<Book>(json)?.let {
                App.db.bookDao().insert(*it.toTypedArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            val file = FileHelp.getFile(path + File.separator + "bookSource.json")
            val json = file.readText()
            GSON.fromJsonArray<BookSource>(json)?.let {
                App.db.bookSourceDao().insert(*it.toTypedArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            val file = FileHelp.getFile(path + File.separator + "rssSource.json")
            val json = file.readText()
            GSON.fromJsonArray<RssSource>(json)?.let {
                App.db.rssSourceDao().insert(*it.toTypedArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            val file = FileHelp.getFile(path + File.separator + "replaceRule.json")
            val json = file.readText()
            GSON.fromJsonArray<ReplaceRule>(json)?.let {
                App.db.replaceRuleDao().insert(*it.toTypedArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importYueDuData(context: Context) {
        val yuedu = File(FileUtils.getSdPath(), "YueDu")

        // 导入书架
        val shelfFile = File(yuedu, "myBookShelf.json")
        val books = mutableListOf<Book>()
        if (shelfFile.exists()) try {
            doAsync {
                val items: List<Map<String, Any>> = jsonPath.parse(shelfFile.readText()).read("$")
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
                    book.type = if (jsonItem.readString("$.bookInfoBean.bookSourceType") == "AUDIO") 1 else 0
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
                    Log.d(AppConst.APP_TAG, "Added ${book.name}")
                }
                App.db.bookDao().insert(*books.toTypedArray())
                val count = books.size

                uiThread {
                    context.toast(if (count > 0) "成功地导入 $count 本新书和音频" else "没有发现新书或音频")
                }

            }

        } catch (e: Exception) {
            Log.e(AppConst.APP_TAG, "Failed to import book shelf.", e)
            context.toast("Unable to import books:\n${e.localizedMessage}")
        }

        // Book source
        val sourceFile = File(yuedu, "myBookSource.json")
        val bookSources = mutableListOf<BookSource>()
        if (sourceFile.exists()) try {
            doAsync {
                val items: List<Map<String, Any>> = jsonPath.parse(sourceFile.readText()).read("$")
                for (item in items) {
                    val jsonItem = jsonPath.parse(item)
                    OldRule.jsonToBookSource(jsonItem.jsonString())?.let {
                        bookSources.add(it)
                    }
                }
                App.db.bookSourceDao().insert(*bookSources.toTypedArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


        // Replace rules
        val ruleFile = File(yuedu, "myBookReplaceRule.json")
        val replaceRules = mutableListOf<ReplaceRule>()
        if (ruleFile.exists()) try {
            doAsync {
                val items: List<Map<String, Any>> = jsonPath.parse(ruleFile.readText()).read("$")
                val existingRules = App.db.replaceRuleDao().all.map { it.pattern }.toSet()
                for ((index: Int, item: Map<String, Any>) in items.withIndex()) {
                    val jsonItem = jsonPath.parse(item)
                    val rule = ReplaceRule()
                    rule.id = jsonItem.readLong("$.id") ?: System.currentTimeMillis().plus(index)
                    rule.pattern = jsonItem.readString("$.regex") ?: ""
                    if (rule.pattern.isEmpty() || rule.pattern in existingRules) continue
                    rule.name = jsonItem.readString("$.replaceSummary") ?: ""
                    rule.replacement = jsonItem.readString("$.replacement") ?: ""
                    rule.isRegex = jsonItem.readBool("$.isRegex") == true
                    rule.scope = jsonItem.readString("$.useTo")
                    rule.isEnabled = jsonItem.readBool("$.enable") == true
                    rule.order = jsonItem.readInt("$.serialNumber") ?: index
                    replaceRules.add(rule)
                }
                App.db.replaceRuleDao().insert(*replaceRules.toTypedArray())
                val count = replaceRules.size
                uiThread {
                    context.toast(if (count > 0) "成功地导入 $count 条净化替换规则" else "没有发现新的净化替换规则")
                }
            }

        } catch (e: Exception) {
            Log.e(AppConst.APP_TAG, e.localizedMessage)
        }
    }
}