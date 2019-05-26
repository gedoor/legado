package io.legado.app.help.storage

import android.content.Context
import android.util.Log
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import io.legado.app.App
import io.legado.app.constant.AppConst
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.utils.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.File

object Restore {

    fun restore() {

    }

    fun importFromGithub() {

    }

    fun importYueDuData(context: Context) {
        val yuedu = File(getSdPath(), "YueDu")
        val jsonPath = JsonPath.using(
            Configuration.builder()
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build()
        )

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
                    book.descUrl = jsonItem.readString("$.noteUrl") ?: ""
                    if (book.descUrl.isBlank()) continue
                    book.name = jsonItem.readString("$.bookInfoBean.name")
                    if (book.descUrl in existingBooks) {
                        Log.d(AppConst.APP_TAG, "Found existing book: ${book.name}")
                        continue
                    }
                    book.author = jsonItem.readString("$.bookInfoBean.author")
                    book.type = if (jsonItem.readString("$.bookInfoBean.bookSourceType") == "AUDIO") 1 else 0
                    book.tocUrl = jsonItem.readString("$.bookInfoBean.chapterUrl") ?: book.descUrl
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
                    // book. = jsonItem.readString("$.hasUpdate")
                    // book. = jsonItem.readString("$.isLoading")
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

        // Replace rules
        val ruleFile = File(yuedu, "myBookReplaceRule.json")
        val replaceRules = mutableListOf<ReplaceRule>()
        if (ruleFile.exists()) try {
            doAsync {
                val items: List<Map<String, Any>> = jsonPath.parse(ruleFile.readText()).read("$")
                val existingRules = App.db.replaceRuleDao().all.map { it.pattern }.toSet()
                for (item in items) {
                    val jsonItem = jsonPath.parse(item)
                    val rule = ReplaceRule()
                    rule.pattern = jsonItem.readString("$.regex")
                    if (rule.pattern.isNullOrEmpty() || rule.pattern in existingRules) continue
                    rule.name = jsonItem.readString("$.replaceSummary")
                    rule.replacement = jsonItem.readString("$.replacement")
                    rule.isRegex = jsonItem.readBool("$.isRegex") == true
                    rule.scope = jsonItem.readString("$.useTo")
                    rule.isEnabled = jsonItem.readBool("$.enable") == true
                    rule.order = jsonItem.readInt("$.serialNumber") ?: 0
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