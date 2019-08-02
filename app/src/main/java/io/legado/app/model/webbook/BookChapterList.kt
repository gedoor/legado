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
import io.legado.app.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import retrofit2.Response

object BookChapterList {

    suspend fun analyzeChapterList(
        coroutineScope: CoroutineScope,
        book: Book,
        response: Response<String>,
        bookSource: BookSource,
        analyzeUrl: AnalyzeUrl
    ): List<BookChapter> {
        val chapterList = arrayListOf<BookChapter>()
        val baseUrl: String = NetworkUtils.getUrl(response)
        val body: String? = response.body()
        body ?: throw Exception(
            App.INSTANCE.getString(
                R.string.get_web_content_error,
                baseUrl
            )
        )
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取成功:$baseUrl")
        val tocRule = bookSource.getTocRule()
        val nextUrlList = arrayListOf(baseUrl)
        var reverse = false
        var listRule = tocRule.chapterList ?: ""
        if (listRule.startsWith("-")) {
            reverse = true
            listRule = listRule.substring(1)
        }
        var chapterData = analyzeChapterList(body, baseUrl, tocRule, listRule, book, bookSource, printLog = true)
        chapterData.chapterList?.let {
            chapterList.addAll(it)
        }
        if (chapterData.nextUrl.size == 1) {
            var nextUrl = chapterData.nextUrl[0]
            while (nextUrl.isNotEmpty() && !nextUrlList.contains(nextUrl)) {
                nextUrlList.add(nextUrl)
                AnalyzeUrl(ruleUrl = nextUrl, book = book).getResponse().execute()
                    .body()?.let { nextBody ->
                        chapterData = analyzeChapterList(nextBody, nextUrl, tocRule, listRule, book, bookSource)
                        nextUrl = if (chapterData.nextUrl.isNotEmpty()) chapterData.nextUrl[0] else ""
                        chapterData.chapterList?.let {
                            chapterList.addAll(it)
                        }
                    }
            }
            if (reverse) chapterList.reverse()
        } else if (chapterData.nextUrl.size > 1) {
            val chapterDataList = arrayListOf<ChapterData<String>>()
            for (item in chapterData.nextUrl) {
                val data = ChapterData(nextUrl = item)
                chapterDataList.add(data)
            }
            for (item in chapterDataList) {
                withContext(coroutineScope.coroutineContext) {
                    val nextResponse = AnalyzeUrl(ruleUrl = item.nextUrl, book = book).getResponseAsync().await()
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
            if (reverse) chapterList.reverse()
        }
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
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取目录下一页列表", printLog)
            analyzeRule.getStringList(tocRule.nextTocUrl ?: "", true)?.let {
                for (item in it) {
                    if (item != baseUrl) {
                        nextUrlList.add(item)
                    }
                }
            }
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, TextUtils.join(",", nextUrlList), printLog)
        }
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, "解析目录列表", printLog)
        val elements = analyzeRule.getElements(listRule)
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, "目录数${elements.size}", printLog)
        if (elements.isNotEmpty()) {
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取目录", printLog)
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
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, "${chapterList[0].title}${chapterList[0].url}", printLog)
        }
        return ChapterData(chapterList, nextUrlList)
    }

}