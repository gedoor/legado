package io.legado.app.model.webbook

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
        val tocRule = bookSource.getTocRule()
        val nextUrlList = arrayListOf(baseUrl)
        var reverse = false
        var listRule = tocRule.chapterList ?: ""
        if (listRule.startsWith("-")) {
            reverse = true
            listRule = listRule.substring(1)
        }
        var chapterData = analyzeChapterList(body, baseUrl, tocRule, listRule, book)
        chapterData.chapterList?.let {
            chapterList.addAll(it)
        }
        if (chapterData.nextUrl.size == 1) {
            var nextUrl = chapterData.nextUrl[0]
            while (nextUrl.isNotEmpty() && !nextUrlList.contains(nextUrl)) {
                nextUrlList.add(nextUrl)
                AnalyzeUrl(ruleUrl = nextUrl, book = book).getResponse().execute()
                    .body()?.let { nextBody ->
                        chapterData = analyzeChapterList(nextBody, nextUrl, tocRule, listRule, book)
                        nextUrl = if (chapterData.nextUrl.isEmpty()) {
                            ""
                        } else {
                            chapterData.nextUrl[0]
                        }
                        chapterData.chapterList?.let {
                            chapterList.addAll(it)
                        }
                    }
            }
            if (reverse) chapterList.reverse()
        } else if (chapterData.nextUrl.size > 1) {
            for (item in chapterData.nextUrl) {
                if (!nextUrlList.contains(item)) {
                    withContext(coroutineScope.coroutineContext) {
                        val nextResponse = AnalyzeUrl(ruleUrl = item, book = book).getResponseAsync().await()
                        val nextChapterData = analyzeChapterList(
                            nextResponse.body() ?: "",
                            item,
                            tocRule,
                            listRule,
                            book
                        )
                        nextChapterData.chapterList?.let {
                            chapterList.addAll(it)
                        }
                    }
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
        book: Book
    ): ChapterData<List<String>> {
        val chapterList = arrayListOf<BookChapter>()
        val nextUrlList = arrayListOf<String>()
        val analyzeRule = AnalyzeRule(book)
        analyzeRule.setContent(body, baseUrl)
        analyzeRule.getStringList(tocRule.nextTocUrl ?: "", true)?.let {
            nextUrlList.addAll(it)
        }
        val elements = analyzeRule.getElements(tocRule.chapterList ?: "")
        if (elements.isNotEmpty()) {
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
        }
        return ChapterData(chapterList, nextUrlList)
    }

}