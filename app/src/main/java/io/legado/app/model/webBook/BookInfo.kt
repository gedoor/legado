package io.legado.app.model.webBook

import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookHelp
import io.legado.app.model.Debug
import io.legado.app.model.NoStackTraceException
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.utils.HtmlFormatter
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.StringUtils.wordCountFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import splitties.init.appCtx

/**
 * 获取详情
 */
object BookInfo {

    @Throws(Exception::class)
    fun analyzeBookInfo(
        scope: CoroutineScope,
        bookSource: BookSource,
        book: Book,
        redirectUrl: String,
        baseUrl: String,
        body: String?,
        canReName: Boolean,
    ) {
        body ?: throw NoStackTraceException(
            appCtx.getString(R.string.error_get_web_content, baseUrl)
        )
        Debug.log(bookSource.bookSourceUrl, "≡获取成功:${baseUrl}")
        Debug.log(bookSource.bookSourceUrl, body, state = 20)
        val analyzeRule = AnalyzeRule(book, bookSource)
        analyzeRule.setContent(body).setBaseUrl(baseUrl)
        analyzeRule.setRedirectUrl(redirectUrl)
        analyzeBookInfo(scope, book, body, analyzeRule, bookSource, baseUrl, redirectUrl, canReName)
    }

    fun analyzeBookInfo(
        scope: CoroutineScope,
        book: Book,
        body: String,
        analyzeRule: AnalyzeRule,
        bookSource: BookSource,
        baseUrl: String,
        redirectUrl: String,
        canReName: Boolean,
    ) {
        val infoRule = bookSource.getBookInfoRule()
        infoRule.init?.let {
            if (it.isNotBlank()) {
                scope.ensureActive()
                Debug.log(bookSource.bookSourceUrl, "≡执行详情页初始化规则")
                analyzeRule.setContent(analyzeRule.getElement(it))
            }
        }
        val mCanReName = canReName && !infoRule.canReName.isNullOrBlank()
        scope.ensureActive()
        Debug.log(bookSource.bookSourceUrl, "┌获取书名")
        BookHelp.formatBookName(analyzeRule.getString(infoRule.name)).let {
            if (it.isNotEmpty() && (mCanReName || book.name.isEmpty())) {
                book.name = it
            }
            Debug.log(bookSource.bookSourceUrl, "└${it}")
        }
        scope.ensureActive()
        Debug.log(bookSource.bookSourceUrl, "┌获取作者")
        BookHelp.formatBookAuthor(analyzeRule.getString(infoRule.author)).let {
            if (it.isNotEmpty() && (mCanReName || book.author.isEmpty())) {
                book.author = it
            }
            Debug.log(bookSource.bookSourceUrl, "└${it}")
        }
        scope.ensureActive()
        Debug.log(bookSource.bookSourceUrl, "┌获取分类")
        try {
            analyzeRule.getStringList(infoRule.kind)
                ?.joinToString(",")
                ?.let {
                    if (it.isNotEmpty()) book.kind = it
                }
            Debug.log(bookSource.bookSourceUrl, "└${book.kind}")
        } catch (e: Exception) {
            Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}")
        }
        scope.ensureActive()
        Debug.log(bookSource.bookSourceUrl, "┌获取字数")
        try {
            wordCountFormat(analyzeRule.getString(infoRule.wordCount)).let {
                if (it.isNotEmpty()) book.wordCount = it
            }
            Debug.log(bookSource.bookSourceUrl, "└${book.wordCount}")
        } catch (e: Exception) {
            Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}")
        }
        scope.ensureActive()
        Debug.log(bookSource.bookSourceUrl, "┌获取最新章节")
        try {
            analyzeRule.getString(infoRule.lastChapter).let {
                if (it.isNotEmpty()) book.latestChapterTitle = it
            }
            Debug.log(bookSource.bookSourceUrl, "└${book.latestChapterTitle}")
        } catch (e: Exception) {
            Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}")
        }
        scope.ensureActive()
        Debug.log(bookSource.bookSourceUrl, "┌获取简介")
        try {
            analyzeRule.getString(infoRule.intro).let {
                if (it.isNotEmpty()) book.intro = HtmlFormatter.format(it)
            }
            Debug.log(bookSource.bookSourceUrl, "└${book.intro}")
        } catch (e: Exception) {
            Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}")
        }
        scope.ensureActive()
        Debug.log(bookSource.bookSourceUrl, "┌获取封面链接")
        try {
            analyzeRule.getString(infoRule.coverUrl).let {
                if (it.isNotEmpty()) book.coverUrl = NetworkUtils.getAbsoluteURL(baseUrl, it)
            }
            Debug.log(bookSource.bookSourceUrl, "└${book.coverUrl}")
        } catch (e: Exception) {
            Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}")
        }
        scope.ensureActive()
        Debug.log(bookSource.bookSourceUrl, "┌获取目录链接")
        book.tocUrl = analyzeRule.getString(infoRule.tocUrl, isUrl = true)
        if (book.tocUrl.isEmpty()) book.tocUrl = redirectUrl
        if (book.tocUrl == redirectUrl) {
            book.tocHtml = body
        }
        Debug.log(bookSource.bookSourceUrl, "└${book.tocUrl}")
    }

}