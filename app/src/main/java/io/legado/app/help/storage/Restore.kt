package io.legado.app.help.storage

import android.content.Context
import android.util.Log
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import io.legado.app.App
import io.legado.app.constant.AppConst
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.data.entities.rule.*
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
        val yuedu = File(FileUtils.getSdPath(), "YueDu")
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
                    book.bookUrl = jsonItem.readString("$.noteUrl") ?: ""
                    if (book.bookUrl.isBlank()) continue
                    book.name = jsonItem.readString("$.bookInfoBean.name")
                    if (book.bookUrl in existingBooks) {
                        Log.d(AppConst.APP_TAG, "Found existing book: ${book.name}")
                        continue
                    }
                    book.author = jsonItem.readString("$.bookInfoBean.author")
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

        // Book source
        val sourceFile = File(yuedu, "myBookSource.json")
        val bookSources = mutableListOf<BookSource>()
        if (shelfFile.exists()) try {
            doAsync {
                val items: List<Map<String, Any>> = jsonPath.parse(sourceFile.readText()).read("$")
                val existingSources = App.db.bookSourceDao().all.map { it.bookSourceUrl }.toSet()
                for (item in items) {
                    val jsonItem = jsonPath.parse(item)
                    val source = BookSource()
                    source.bookSourceUrl = jsonItem.readString("bookSourceUrl") ?: ""
                    if (source.bookSourceUrl.isBlank()) continue
                    if (source.bookSourceUrl in existingSources) continue
                    source.bookSourceName = jsonItem.readString("bookSourceName") ?: ""
                    source.bookSourceGroup = jsonItem.readString("bookSourceGroup") ?: ""
                    source.loginUrl = jsonItem.readString("loginUrl")
                    source.bookUrlPattern = jsonItem.readString("ruleBookUrlPattern")
                    val searchRule = SearchRule(
                        searchUrl = jsonItem.readString("ruleSearchUrl"),
                        bookList = jsonItem.readString("ruleSearchList"),
                        name = jsonItem.readString("ruleSearchName"),
                        author = jsonItem.readString("ruleSearchAuthor"),
                        intro = jsonItem.readString("ruleSearchIntroduce"),
                        kind = jsonItem.readString("ruleSearchKind"),
                        bookUrl = jsonItem.readString("ruleSearchNoteUrl"),
                        coverUrl = jsonItem.readString("ruleSearchCoverUrl"),
                        lastChapter = jsonItem.readString("ruleSearchLastChapter")
                    )
                    source.ruleSearch = GSON.toJson(searchRule)
                    val exploreRule = ExploreRule(
                        exploreUrl = jsonItem.readString("ruleFindUrl"),
                        bookList = jsonItem.readString("ruleFindList"),
                        name = jsonItem.readString("ruleFindName"),
                        author = jsonItem.readString("ruleFindAuthor"),
                        intro = jsonItem.readString("ruleFindIntroduce"),
                        kind = jsonItem.readString("ruleFindKind"),
                        bookUrl = jsonItem.readString("ruleFindNoteUrl"),
                        coverUrl = jsonItem.readString("ruleFindCoverUrl"),
                        lastChapter = jsonItem.readString("ruleFindLastChapter")
                    )
                    source.ruleExplore = GSON.toJson(exploreRule)
                    val bookInfoRule = BookInfoRule(
                        init = jsonItem.readString("ruleBookInfoInit"),
                        name = jsonItem.readString("ruleBookName"),
                        author = jsonItem.readString("ruleBookAuthor"),
                        intro = jsonItem.readString("ruleIntroduce"),
                        kind = jsonItem.readString("ruleBookKind"),
                        coverUrl = jsonItem.readString("ruleCoverUrl"),
                        lastChapter = jsonItem.readString("ruleBookLastChapter"),
                        tocUrl = jsonItem.readString("ruleChapterUrl")
                    )
                    source.ruleBookInfo = GSON.toJson(bookInfoRule)
                    val chapterRule = TocRule(
                        chapterList = jsonItem.readString("ruleChapterUrlNext"),
                        chapterName = jsonItem.readString("ruleChapterName"),
                        chapterUrl = jsonItem.readString("ruleContentUrl"),
                        nextTocUrl = jsonItem.readString("ruleChapterUrlNext")
                    )
                    source.ruleToc = GSON.toJson(chapterRule)
                    val contentRule = ContentRule(
                        content = jsonItem.readString("ruleBookContent"),
                        nextContentUrl = jsonItem.readString("ruleContentUrlNext")
                    )
                    source.ruleContent = GSON.toJson(contentRule)
                    bookSources.add(source)
                }
                App.db.bookSourceDao().insert(*bookSources.toTypedArray())
            }
        } catch (e: Exception) {
            error(e.localizedMessage)
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