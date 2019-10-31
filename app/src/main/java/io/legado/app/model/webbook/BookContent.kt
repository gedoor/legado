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

object BookContent {

    @Throws(Exception::class)
    suspend fun analyzeContent(
        coroutineScope: CoroutineScope,
        body: String?,
        book: Book,
        bookChapter: BookChapter,
        bookSource: BookSource,
        baseUrl: String,
        nextChapterUrlF: String? = null
    ): String {
        body ?: throw Exception(
            App.INSTANCE.getString(
                R.string.error_get_web_content,
                baseUrl
            )
        )
        SourceDebug.printLog(bookSource.bookSourceUrl, "获取成功:${baseUrl}")
        val content = StringBuilder()
        val nextUrlList = arrayListOf(baseUrl)
        val contentRule = bookSource.getContentRule()
        var contentData = analyzeContent(body, contentRule, book, bookSource, baseUrl)
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
                AnalyzeUrl(
                    ruleUrl = nextUrl,
                    book = book,
                    headerMapF = bookSource.getHeaderMap()
                ).getResponseAsync().await()
                    .body()?.let { nextBody ->
                        contentData =
                            analyzeContent(nextBody, contentRule, book, bookSource, baseUrl, false)
                        nextUrl =
                            if (contentData.nextUrl.isNotEmpty()) contentData.nextUrl[0] else ""
                        content.append(contentData.content)
                    }
            }
            SourceDebug.printLog(bookSource.bookSourceUrl, "下一页数量${nextUrlList.size}")
        } else if (contentData.nextUrl.size > 1) {
            val contentDataList = arrayListOf<ContentData<String>>()
            for (item in contentData.nextUrl) {
                if (!nextUrlList.contains(item))
                    contentDataList.add(ContentData(nextUrl = item))
            }
            for (item in contentDataList) {
                withContext(coroutineScope.coroutineContext) {
                    AnalyzeUrl(
                        ruleUrl = item.nextUrl,
                        book = book,
                        headerMapF = bookSource.getHeaderMap()
                    ).getResponseAsync().await()
                        .body()?.let {
                            contentData =
                                analyzeContent(
                                    it,
                                    contentRule,
                                    book,
                                    bookSource,
                                    item.nextUrl,
                                    false
                                )
                            item.content = contentData.content
                        }
                }
            }
            for (item in contentDataList) {
                content.append(item.content)
            }
        }
        if (content.isNotEmpty()) {
            if (!content[0].toString().startsWith(bookChapter.title)) {
                content
                    .insert(0, "\n")
                    .insert(0, bookChapter.title)
            }
        }
        return content.toString()
    }

    @Throws(Exception::class)
    private fun analyzeContent(
        body: String,
        contentRule: ContentRule,
        book: Book,
        bookSource: BookSource,
        baseUrl: String,
        printLog: Boolean = true
    ): ContentData<List<String>> {
        val nextUrlList = arrayListOf<String>()
        val analyzeRule = AnalyzeRule(book)
        analyzeRule.setContent(body, baseUrl)
        val nextUrlRule = contentRule.nextContentUrl
        if (!nextUrlRule.isNullOrEmpty()) {
            SourceDebug.printLog(bookSource.bookSourceUrl, "获取下一页URL", printLog)
            analyzeRule.getStringList(nextUrlRule, true)?.let {
                nextUrlList.addAll(it)
            }
            SourceDebug.printLog(bookSource.bookSourceUrl, nextUrlList.joinToString(","), printLog)
        }
        val content = analyzeRule.getString(contentRule.content ?: "")?.htmlFormat() ?: ""
        return ContentData(content, nextUrlList)
    }
}