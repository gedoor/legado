package io.legado.app.model.webbook

import android.text.TextUtils
import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.TocRule
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.time.DateUtils

object BookChapterList {

    suspend fun analyzeChapterList(
        coroutineScope: CoroutineScope,
        book: Book,
        body: String?,
        bookSource: BookSource,
        baseUrl: String
    ): List<BookChapter> {
        var chapterList = arrayListOf<BookChapter>()
        body ?: throw Exception(
            App.INSTANCE.getString(R.string.error_get_web_content, baseUrl)
        )
        SourceDebug.printLog(bookSource.bookSourceUrl, "≡获取成功:${baseUrl}")
        val tocRule = bookSource.getTocRule()
        val nextUrlList = arrayListOf(baseUrl)
        var reverse = false
        var listRule = tocRule.chapterList ?: ""
        if (listRule.startsWith("-")) {
            reverse = true
            listRule = listRule.substring(1)
        }
        if (listRule.startsWith("+")) {
            listRule = listRule.substring(1)
        }
        var chapterData =
            analyzeChapterList(body, baseUrl, tocRule, listRule, book, bookSource, printLog = true)
        chapterData.chapterList?.let {
            chapterList.addAll(it)
        }
        if (chapterData.nextUrl.size == 1) {
            var nextUrl = chapterData.nextUrl[0]
            while (nextUrl.isNotEmpty() && !nextUrlList.contains(nextUrl)) {
                nextUrlList.add(nextUrl)
                AnalyzeUrl(
                    ruleUrl = nextUrl,
                    book = book,
                    headerMapF = bookSource.getHeaderMap()
                ).getResponseAwait()
                    .body()?.let { nextBody ->
                        chapterData = analyzeChapterList(
                            nextBody,
                            nextUrl,
                            tocRule,
                            listRule,
                            book,
                            bookSource,
                            printLog = false
                        )
                        nextUrl = if (chapterData.nextUrl.isNotEmpty())
                            chapterData.nextUrl[0]
                        else ""
                        chapterData.chapterList?.let {
                            chapterList.addAll(it)
                        }
                    }
            }
            SourceDebug.printLog(bookSource.bookSourceUrl, "◇目录总页数:${nextUrlList.size}")
        } else if (chapterData.nextUrl.size > 1) {
            val chapterDataList = arrayListOf<ChapterData<String>>()
            for (item in chapterData.nextUrl) {
                val data = ChapterData(nextUrl = item)
                chapterDataList.add(data)
            }
            for (item in chapterDataList) {
                withContext(coroutineScope.coroutineContext) {
                    val nextResponse = AnalyzeUrl(
                        ruleUrl = item.nextUrl,
                        book = book,
                        headerMapF = bookSource.getHeaderMap()
                    ).getResponseAwait()
                    val nextChapterData = analyzeChapterList(
                        nextResponse.body() ?: "",
                        item.nextUrl,
                        tocRule,
                        listRule,
                        book,
                        bookSource
                    )
                    item.chapterList = nextChapterData.chapterList
                }
            }
            for (item in chapterDataList) {
                item.chapterList?.let {
                    chapterList.addAll(it)
                }
            }
        }
        //去重
        if (!reverse) {
            chapterList.reverse()
        }
        val lh = LinkedHashSet(chapterList)
        chapterList = ArrayList(lh)
        chapterList.reverse()
        for ((index, item) in chapterList.withIndex()) {
            item.index = index
        }
        book.latestChapterTitle = chapterList.last().title
        if (book.totalChapterNum < chapterList.size) {
            book.lastCheckCount = chapterList.size - book.totalChapterNum
        }
        book.totalChapterNum = chapterList.size
        return chapterList
    }


    private fun analyzeChapterList(
        body: String,
        baseUrl: String,
        tocRule: TocRule,
        listRule: String,
        book: Book,
        bookSource: BookSource,
        getNextUrl: Boolean = true,
        printLog: Boolean = false
    ): ChapterData<List<String>> {
        val chapterList = arrayListOf<BookChapter>()
        val nextUrlList = arrayListOf<String>()
        val analyzeRule = AnalyzeRule(book)
        analyzeRule.setContent(body, baseUrl)
        val nextTocRule = tocRule.nextTocUrl
        if (getNextUrl && !nextTocRule.isNullOrEmpty()) {
            SourceDebug.printLog(bookSource.bookSourceUrl, "┌获取目录下一页列表", printLog)
            analyzeRule.getStringList(nextTocRule, true)?.let {
                for (item in it) {
                    if (item != baseUrl) {
                        nextUrlList.add(item)
                    }
                }
            }
            SourceDebug.printLog(
                bookSource.bookSourceUrl,
                "└" + TextUtils.join("，\n", nextUrlList),
                printLog
            )
        }
        SourceDebug.printLog(bookSource.bookSourceUrl, "┌获取目录列表", printLog)
        val elements = analyzeRule.getElements(listRule)
        SourceDebug.printLog(bookSource.bookSourceUrl, "└列表大小:${elements.size}", printLog)
        if (elements.isNotEmpty()) {
            SourceDebug.printLog(bookSource.bookSourceUrl, "┌获取首章名称", printLog)
            val nameRule = analyzeRule.splitSourceRule(tocRule.chapterName)
            val urlRule = analyzeRule.splitSourceRule(tocRule.chapterUrl)
            val vipRule = analyzeRule.splitSourceRule(tocRule.isVip)
            val update = analyzeRule.splitSourceRule(tocRule.updateTime)
            var isVip: String?
            var timeStr: String
            for (item in elements) {
                analyzeRule.setContent(item)
                val bookChapter = BookChapter(bookUrl = book.bookUrl)
                analyzeRule.chapter = bookChapter
                bookChapter.title = analyzeRule.getString(nameRule)
                bookChapter.url = analyzeRule.getString(urlRule, true)
                timeStr = analyzeRule.getString(update)
                isVip = analyzeRule.getString(vipRule)
                if (bookChapter.url.isEmpty()) bookChapter.url = baseUrl
                if (bookChapter.title.isNotEmpty()) {
                    if (isVip.isNotEmpty() && isVip != "null" && isVip != "false" && isVip != "0") {
                        bookChapter.title = "\uD83D\uDD12" + bookChapter.title
                    }
                    tocRule.timeFormat?.let {
                        if (it.isNotEmpty()) {
                            kotlin.runCatching {
                                bookChapter.start = DateUtils.parseDate(timeStr, it).time
                            }
                        }
                    }
                    chapterList.add(bookChapter)
                }
            }
            SourceDebug.printLog(bookSource.bookSourceUrl, "└${chapterList[0].title}", printLog)
            SourceDebug.printLog(bookSource.bookSourceUrl, "┌获取首章链接", printLog)
            SourceDebug.printLog(bookSource.bookSourceUrl, "└${chapterList[0].url}", printLog)
        }
        return ChapterData(chapterList, nextUrlList)
    }

}