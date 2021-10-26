package io.legado.app.model.webBook

import io.legado.app.R
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.help.BookHelp
import io.legado.app.model.ContentEmptyException
import io.legado.app.model.Debug
import io.legado.app.model.NoStackTraceException
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.HtmlFormatter
import io.legado.app.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import splitties.init.appCtx

/**
 * 获取正文
 */
object BookContent {

    @Throws(Exception::class)
    suspend fun analyzeContent(
        scope: CoroutineScope,
        bookSource: BookSource,
        book: Book,
        bookChapter: BookChapter,
        redirectUrl: String,
        baseUrl: String,
        body: String?,
        nextChapterUrl: String? = null
    ): String {
        body ?: throw NoStackTraceException(
            appCtx.getString(R.string.error_get_web_content, baseUrl)
        )
        Debug.log(bookSource.bookSourceUrl, "≡获取成功:${baseUrl}")
        Debug.log(bookSource.bookSourceUrl, body, state = 40)
        val mNextChapterUrl = if (!nextChapterUrl.isNullOrEmpty()) {
            nextChapterUrl
        } else {
            appDb.bookChapterDao.getChapter(book.bookUrl, bookChapter.index + 1)?.url
        }
        val content = StringBuilder()
        val nextUrlList = arrayListOf(baseUrl)
        val contentRule = bookSource.getContentRule()
        val analyzeRule = AnalyzeRule(book, bookSource).setContent(body, baseUrl)
        analyzeRule.setRedirectUrl(baseUrl)
        analyzeRule.nextChapterUrl = mNextChapterUrl
        scope.ensureActive()
        var contentData = analyzeContent(
            book, baseUrl, redirectUrl, body, contentRule, bookChapter, bookSource, mNextChapterUrl
        )
        content.append(contentData.first)
        if (contentData.second.size == 1) {
            var nextUrl = contentData.second[0]
            while (nextUrl.isNotEmpty() && !nextUrlList.contains(nextUrl)) {
                if (!mNextChapterUrl.isNullOrEmpty()
                    && NetworkUtils.getAbsoluteURL(baseUrl, nextUrl)
                    == NetworkUtils.getAbsoluteURL(baseUrl, mNextChapterUrl)
                ) break
                nextUrlList.add(nextUrl)
                scope.ensureActive()
                val res = AnalyzeUrl(
                    mUrl = nextUrl,
                    source = bookSource,
                    ruleData = book,
                    headerMapF = bookSource.getHeaderMap()
                ).getStrResponseAwait()
                res.body?.let { nextBody ->
                    contentData = analyzeContent(
                        book, nextUrl, res.url, nextBody, contentRule,
                        bookChapter, bookSource, mNextChapterUrl, false
                    )
                    nextUrl =
                        if (contentData.second.isNotEmpty()) contentData.second[0] else ""
                    content.append("\n").append(contentData.first)
                }
            }
            Debug.log(bookSource.bookSourceUrl, "◇本章总页数:${nextUrlList.size}")
        } else if (contentData.second.size > 1) {
            Debug.log(bookSource.bookSourceUrl, "◇并发解析目录,总页数:${contentData.second.size}")
            withContext(IO) {
                val asyncArray = Array(contentData.second.size) {
                    async(IO) {
                        val urlStr = contentData.second[it]
                        val analyzeUrl = AnalyzeUrl(
                            mUrl = urlStr,
                            source = bookSource,
                            ruleData = book,
                            headerMapF = bookSource.getHeaderMap()
                        )
                        val res = analyzeUrl.getStrResponseAwait()
                        analyzeContent(
                            book, urlStr, res.url, res.body!!, contentRule,
                            bookChapter, bookSource, mNextChapterUrl, false
                        ).first
                    }
                }
                asyncArray.forEach { coroutine ->
                    scope.ensureActive()
                    content.append("\n").append(coroutine.await())
                }
            }
        }
        var contentStr = content.toString()
        val replaceRegex = contentRule.replaceRegex
        if (!replaceRegex.isNullOrEmpty()) {
            contentStr = analyzeRule.getString(replaceRegex, contentStr)
        }
        Debug.log(bookSource.bookSourceUrl, "┌获取章节名称")
        Debug.log(bookSource.bookSourceUrl, "└${bookChapter.title}")
        Debug.log(bookSource.bookSourceUrl, "┌获取正文内容")
        Debug.log(bookSource.bookSourceUrl, "└\n$contentStr")
        if (contentStr.isBlank()) {
            throw ContentEmptyException("内容为空")
        }
        BookHelp.saveContent(bookSource, book, bookChapter, contentStr)
        return contentStr
    }

    @Throws(Exception::class)
    private fun analyzeContent(
        book: Book,
        baseUrl: String,
        redirectUrl: String,
        body: String,
        contentRule: ContentRule,
        chapter: BookChapter,
        bookSource: BookSource,
        nextChapterUrl: String?,
        printLog: Boolean = true
    ): Pair<String, List<String>> {
        val analyzeRule = AnalyzeRule(book, bookSource)
        analyzeRule.setContent(body, baseUrl)
        val rUrl = analyzeRule.setRedirectUrl(redirectUrl)
        analyzeRule.nextChapterUrl = nextChapterUrl
        val nextUrlList = arrayListOf<String>()
        analyzeRule.chapter = chapter
        //获取正文
        var content = analyzeRule.getString(contentRule.content)
        content = HtmlFormatter.formatKeepImg(content, rUrl)
        //获取下一页链接
        val nextUrlRule = contentRule.nextContentUrl
        if (!nextUrlRule.isNullOrEmpty()) {
            Debug.log(bookSource.bookSourceUrl, "┌获取正文下一页链接", printLog)
            analyzeRule.getStringList(nextUrlRule, isUrl = true)?.let {
                nextUrlList.addAll(it)
            }
            Debug.log(bookSource.bookSourceUrl, "└" + nextUrlList.joinToString("，"), printLog)
        }
        return Pair(content, nextUrlList)
    }
}
