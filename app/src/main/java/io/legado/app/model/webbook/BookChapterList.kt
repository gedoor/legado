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
import kotlinx.coroutines.launch
import retrofit2.Response

object BookChapterList {

    fun analyzeChapterList(
        coroutineScope: CoroutineScope,
        book: Book,
        response: Response<String>,
        bookSource: BookSource,
        analyzeUrl: AnalyzeUrl,
        success: (List<BookChapter>) -> Unit
    ) {
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
        var chapterData = analyzeChapterList(body, baseUrl, tocRule, book)
        chapterData.chapterList?.let {
            chapterList.addAll(it)
        }
        if (chapterData.nextUrl.size == 1) {
            var nextUrl = chapterData.nextUrl[0]
            while (nextUrl.isNotEmpty() && !nextUrlList.contains(nextUrl)) {
                nextUrlList.add(nextUrl)
                AnalyzeUrl(ruleUrl = nextUrl, book = book).getResponse().execute()
                    .body()?.let { nextBody ->
                        chapterData = analyzeChapterList(nextBody, nextUrl, tocRule, book)
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
            success(chapterList)
        } else if (chapterData.nextUrl.size > 1) {
            val chapterDataList = arrayListOf<ChapterData<String>>()
            for (item in chapterData.nextUrl) {
                if (!nextUrlList.contains(item)) {
                    val data = ChapterData(nextUrl = item)
                    chapterDataList.add(data)
                }
            }
            var successCount = 0
            for (item in chapterDataList) {
                coroutineScope.launch {
                    AnalyzeUrl(ruleUrl = item.nextUrl, book = book).getResponse().execute().body()
                }
            }
        }
    }


    private fun analyzeChapterList(
        body: String,
        baseUrl: String,
        tocRule: TocRule,
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