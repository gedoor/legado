package io.legado.app.model.webBook

import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.help.BookHelp
import io.legado.app.model.Debug
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.htmlFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

object BookContent {

    @Throws(Exception::class)
    suspend fun analyzeContent(
        scope: CoroutineScope,
        body: String?,
        book: Book,
        bookChapter: BookChapter,
        bookSource: BookSource,
        baseUrl: String,
        nextChapterUrlF: String? = null
    ): String {
        body ?: throw Exception(
            App.INSTANCE.getString(R.string.error_get_web_content, baseUrl)
        )
        Debug.log(bookSource.bookSourceUrl, "≡获取成功:${baseUrl}")
        val content = StringBuilder()
        val nextUrlList = arrayListOf(baseUrl)
        val contentRule = bookSource.getContentRule()
        val analyzeRule = AnalyzeRule(book).setContent(body, baseUrl)
        var contentData = analyzeContent(
            book, baseUrl, body, contentRule, bookChapter, bookSource
        )
        content.append(contentData.content).append("\n")

        if (contentData.nextUrl.size == 1) {
            var nextUrl = contentData.nextUrl[0]
            val nextChapterUrl = if (!nextChapterUrlF.isNullOrEmpty())
                nextChapterUrlF
            else
                App.db.bookChapterDao.getChapter(book.bookUrl, bookChapter.index + 1)?.url
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
                ).getStrResponse(bookSource.bookSourceUrl).body?.let { nextBody ->
                    contentData = analyzeContent(
                        book, nextUrl, nextBody, contentRule, bookChapter, bookSource, false
                    )
                    nextUrl =
                        if (contentData.nextUrl.isNotEmpty()) contentData.nextUrl[0] else ""
                    content.append(contentData.content).append("\n")
                }
            }
            Debug.log(bookSource.bookSourceUrl, "◇本章总页数:${nextUrlList.size}")
        } else if (contentData.nextUrl.size > 1) {
            val contentDataList = arrayListOf<ContentData<String>>()
            for (item in contentData.nextUrl) {
                if (!nextUrlList.contains(item))
                    contentDataList.add(ContentData(nextUrl = item))
            }
            for (item in contentDataList) {
                withContext(scope.coroutineContext) {
                    AnalyzeUrl(
                        ruleUrl = item.nextUrl,
                        book = book,
                        headerMapF = bookSource.getHeaderMap()
                    ).getStrResponse(bookSource.bookSourceUrl).body?.let {
                        contentData = analyzeContent(
                            book, item.nextUrl, it, contentRule, bookChapter, bookSource, false
                        )
                        item.content = contentData.content
                    }
                }
            }
            for (item in contentDataList) {
                content.append(item.content).append("\n")
            }
        }
        content.deleteCharAt(content.length - 1)
        var contentStr = content.toString().htmlFormat()
        val replaceRegex = contentRule.replaceRegex
        if (!replaceRegex.isNullOrEmpty()) {
            contentStr = analyzeRule.getString(replaceRegex, value = contentStr)
        }
        Debug.log(bookSource.bookSourceUrl, "┌获取章节名称")
        Debug.log(bookSource.bookSourceUrl, "└${bookChapter.title}")
        Debug.log(bookSource.bookSourceUrl, "┌获取正文内容")
        Debug.log(bookSource.bookSourceUrl, "└\n$contentStr")
        if (contentStr.isNotBlank()) {
            BookHelp.saveContent(book, bookChapter, contentStr)
        }
        return contentStr
    }

    @Throws(Exception::class)
    private fun analyzeContent(
        book: Book,
        baseUrl: String,
        body: String,
        contentRule: ContentRule,
        chapter: BookChapter,
        bookSource: BookSource,
        printLog: Boolean = true
    ): ContentData<List<String>> {
        val analyzeRule = AnalyzeRule(book)
        analyzeRule.setContent(body).setBaseUrl(baseUrl)
        val nextUrlList = arrayListOf<String>()
        analyzeRule.chapter = chapter
        val nextUrlRule = contentRule.nextContentUrl
        if (!nextUrlRule.isNullOrEmpty()) {
            Debug.log(bookSource.bookSourceUrl, "┌获取正文下一页链接", printLog)
            analyzeRule.getStringList(nextUrlRule, true)?.let {
                nextUrlList.addAll(it)
            }
            Debug.log(bookSource.bookSourceUrl, "└" + nextUrlList.joinToString("，"), printLog)
        }
        val content = analyzeRule.getString(contentRule.content)
        return ContentData(content, nextUrlList)
    }
}