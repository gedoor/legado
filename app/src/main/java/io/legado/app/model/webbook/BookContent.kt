package io.legado.app.model.webbook

import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.htmlFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import retrofit2.Response

object BookContent {

    @Throws(Exception::class)
    suspend fun analyzeContent(
        coroutineScope: CoroutineScope,
        response: Response<String>,
        book: Book,
        bookChapter: BookChapter,
        bookSource: BookSource,
        nextChapterUrlF: String? = null
    ): String {
        val baseUrl: String = NetworkUtils.getUrl(response)
        val body: String? = response.body()
        body ?: throw Exception(
            App.INSTANCE.getString(
                R.string.error_get_web_content,
                baseUrl
            )
        )
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取成功:$baseUrl")
        val content = StringBuilder()
        val nextUrlList = arrayListOf(baseUrl)
        val contentRule = bookSource.getContentRule()
        var contentData = analyzeContent(body, contentRule, book, baseUrl)
        content.append(contentData.content)
        if (contentData.nextUrl.size == 1) {
            var nextUrl = contentData.nextUrl[0]
            val nextChapterUrl = if (!nextChapterUrlF.isNullOrEmpty())
                nextChapterUrlF
            else
                App.db.bookChapterDao().getChapter(book.bookUrl, bookChapter.index + 1)?.url
            while (nextUrl.isNotEmpty() && !nextUrlList.contains(nextUrl)) {
                if (!nextChapterUrl.isNullOrEmpty()
                    && NetworkUtils.getAbsoluteURL(baseUrl, nextUrl)
                    == NetworkUtils.getAbsoluteURL(baseUrl, nextChapterUrl)
                ) break
                nextUrlList.add(nextUrl)
                AnalyzeUrl(ruleUrl = nextUrl, book = book).getResponseAsync().await()
                    .body()?.let { nextBody ->
                        contentData = analyzeContent(nextBody, contentRule, book, baseUrl)
                        nextUrl = if (contentData.nextUrl.isNotEmpty()) contentData.nextUrl[0] else ""
                        content.append(contentData.content)
                    }
            }
        } else if (contentData.nextUrl.size > 1) {
            val contentDataList = arrayListOf<ContentData<String>>()
            for (item in contentData.nextUrl) {
                if (!nextUrlList.contains(item))
                    contentDataList.add(ContentData(nextUrl = item))
            }
            for (item in contentDataList) {
                withContext(coroutineScope.coroutineContext) {
                    val nextResponse = AnalyzeUrl(ruleUrl = item.nextUrl, book = book).getResponseAsync().await()
                    val nextContentData = analyzeContent(
                        nextResponse.body() ?: "",
                        contentRule,
                        book,
                        item.nextUrl
                    )
                    item.content = nextContentData.content
                }
            }
            for (item in contentDataList) {
                content.append(item.content)
            }
        }
        if (content.isNotEmpty()) {
            if (!content[0].toString().startsWith(bookChapter.title)) {
                content.insert(0, bookChapter.title)
            }
        }
        return content.toString()
    }

    @Throws(Exception::class)
    private fun analyzeContent(
        body: String,
        contentRule: ContentRule,
        book: Book,
        baseUrl: String
    ): ContentData<List<String>> {
        val nextUrlList = arrayListOf<String>()
        val analyzeRule = AnalyzeRule(book)
        analyzeRule.setContent(body, baseUrl)
        analyzeRule.getStringList(contentRule.nextContentUrl ?: "", true)?.let {
            nextUrlList.addAll(it)
        }
        val content = analyzeRule.getString(contentRule.content ?: "")?.htmlFormat() ?: ""
        return ContentData(content, nextUrlList)
    }
}