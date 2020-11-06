package io.legado.app.model.webBook

import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookHelp
import io.legado.app.model.Debug
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.StringUtils.wordCountFormat
import io.legado.app.utils.htmlFormat

object BookInfo {

    @Throws(Exception::class)
    fun analyzeBookInfo(
        book: Book,
        body: String?,
        bookSource: BookSource,
        baseUrl: String,
        canReName: Boolean,
    ) {
        body ?: throw Exception(
            App.INSTANCE.getString(R.string.error_get_web_content, baseUrl)
        )
        Debug.log(bookSource.bookSourceUrl, "≡获取成功:${baseUrl}")
        val infoRule = bookSource.getBookInfoRule()
        val analyzeRule = AnalyzeRule(book)
        analyzeRule.setContent(body).setBaseUrl(baseUrl)
        infoRule.init?.let {
            if (it.isNotEmpty()) {
                Debug.log(bookSource.bookSourceUrl, "≡执行详情页初始化规则")
                analyzeRule.setContent(analyzeRule.getElement(it))
            }
        }
        Debug.log(bookSource.bookSourceUrl, "┌获取书名")
        BookHelp.formatBookName(analyzeRule.getString(infoRule.name)).let {
            if (it.isNotEmpty() && (canReName || book.name.isEmpty())) {
                book.name = it
            }
        }
        Debug.log(bookSource.bookSourceUrl, "└${book.name}")
        Debug.log(bookSource.bookSourceUrl, "┌获取作者")
        BookHelp.formatBookAuthor(analyzeRule.getString(infoRule.author)).let {
            if (it.isNotEmpty() && (canReName || book.name.isEmpty())) {
                book.author = it
            }
        }
        Debug.log(bookSource.bookSourceUrl, "└${book.author}")
        Debug.log(bookSource.bookSourceUrl, "┌获取分类")
        analyzeRule.getStringList(infoRule.kind)
            ?.joinToString(",")
            ?.let {
                if (it.isNotEmpty()) book.kind = it
            }
        Debug.log(bookSource.bookSourceUrl, "└${book.kind}")
        Debug.log(bookSource.bookSourceUrl, "┌获取字数")
        wordCountFormat(analyzeRule.getString(infoRule.wordCount)).let {
            if (it.isNotEmpty()) book.wordCount = it
        }
        Debug.log(bookSource.bookSourceUrl, "└${book.wordCount}")
        Debug.log(bookSource.bookSourceUrl, "┌获取最新章节")
        analyzeRule.getString(infoRule.lastChapter).let {
            if (it.isNotEmpty()) book.latestChapterTitle = it
        }
        Debug.log(bookSource.bookSourceUrl, "└${book.latestChapterTitle}")
        Debug.log(bookSource.bookSourceUrl, "┌获取简介")
        analyzeRule.getString(infoRule.intro).let {
            if (it.isNotEmpty()) book.intro = it.htmlFormat()
        }
        Debug.log(bookSource.bookSourceUrl, "└${book.intro}", isHtml = true)

        Debug.log(bookSource.bookSourceUrl, "┌获取封面链接")
        analyzeRule.getString(infoRule.coverUrl).let {
            if (it.isNotEmpty()) book.coverUrl = NetworkUtils.getAbsoluteURL(baseUrl, it)
        }
        Debug.log(bookSource.bookSourceUrl, "└${book.coverUrl}")
        Debug.log(bookSource.bookSourceUrl, "┌获取目录链接")
        book.tocUrl = analyzeRule.getString(infoRule.tocUrl, true)
        if (book.tocUrl.isEmpty()) book.tocUrl = baseUrl
        if (book.tocUrl == baseUrl) {
            book.tocHtml = body
        }
        Debug.log(bookSource.bookSourceUrl, "└${book.tocUrl}")
    }

}