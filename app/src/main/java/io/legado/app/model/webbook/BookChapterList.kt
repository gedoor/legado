package io.legado.app.model.webbook

import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.TocRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.NetworkUtils
import retrofit2.Response

class BookChapterList {

    fun analyzeChapterList(
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
        val chapterData = analyzeChapterList(body, tocRule)
        chapterList.addAll(chapterData.chapterList)
        if (chapterData.nextUrlList.size == 1) {
            var nextUrl = chapterData.nextUrlList[0]
            while (nextUrl.isNotEmpty() && !nextUrlList.contains(nextUrl)) {
                nextUrlList.add(nextUrl)

            }
        }

        return chapterList
    }

    private fun analyzeChapterList(body: String, tocRule: TocRule): ChapterData {
        val chapterList = arrayListOf<BookChapter>()
        val nextUrlList = arrayListOf<String>()


        return ChapterData(chapterList, nextUrlList)
    }

}