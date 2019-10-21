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
        SourceDebug.printLog(bookSource.bookSourceUrl, "获取成功:${baseUrl}")
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
                ).getResponseAsync().await()
                    .body()?.let { nextBody ->
                        chapterData = analyzeChapterList(
                            nextBody,
                            nextUrl,
                            tocRule,
                            listRule,
                            book,
                            bookSource
                        )
                        nextUrl = if (chapterData.nextUrl.isNotEmpty())
                            chapterData.nextUrl[0]
                        else ""
                        chapterData.chapterList?.let {
                            chapterList.addAll(it)
                        }
                    }
            }
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
                    ).getResponseAsync().await()
                    val nextChapterData = analyzeChapterList(
                        nextResponse.body() ?: "",
                        item.nextUrl,
                        tocRule,
                        listRule,
                        book,
                        bookSource,
                        getNextUrl = false
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
        if (getNextUrl) {
            SourceDebug.printLog(bookSource.bookSourceUrl, "获取目录下一页列表", print = printLog)
            analyzeRule.getStringList(tocRule.nextTocUrl ?: "", true)?.let {
                for (item in it) {
                    if (item != baseUrl) {
                        nextUrlList.add(item)
                    }
                }
            }
            SourceDebug.printLog(
                bookSource.bookSourceUrl,
                TextUtils.join(",", nextUrlList),
                print = printLog
            )
        }
        SourceDebug.printLog(bookSource.bookSourceUrl, "解析目录列表", print = printLog)
        val elements = analyzeRule.getElements(listRule)
        SourceDebug.printLog(bookSource.bookSourceUrl, "目录数${elements.size}", print = printLog)
        if (elements.isNotEmpty()) {
            SourceDebug.printLog(bookSource.bookSourceUrl, "获取目录", print = printLog)
            val nameRule = analyzeRule.splitSourceRule(tocRule.chapterName ?: "")
            val urlRule = analyzeRule.splitSourceRule(tocRule.chapterUrl ?: "")
            for (item in elements) {
                analyzeRule.setContent(item)
                val title = analyzeRule.getString(nameRule)
                if (title.isNotEmpty()) {
                    val bookChapter = BookChapter(bookUrl = book.bookUrl)
                    bookChapter.title = title
                    bookChapter.url = analyzeRule.getString(urlRule, true)
                    if (bookChapter.url.isEmpty()) bookChapter.url = baseUrl
                    chapterList.add(bookChapter)
                }
            }
            SourceDebug.printLog(
                bookSource.bookSourceUrl,
                "${chapterList[0].title}${chapterList[0].url}",
                print = printLog
            )
        }
        return ChapterData(chapterList, nextUrlList)
    }

}