package io.legado.app.model.webBook

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.Debug
import io.legado.app.model.analyzeRule.AnalyzeUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

class WebBook(val bookSource: BookSource) {

    val sourceUrl: String
        get() = bookSource.bookSourceUrl

    /**
     * 搜索
     */
    fun searchBook(
        key: String,
        page: Int? = 1,
        variableBook: SearchBook,
        scope: CoroutineScope = Coroutine.DEFAULT,
        context: CoroutineContext = Dispatchers.IO,
    ): Coroutine<ArrayList<SearchBook>> {
        return Coroutine.async(scope, context) {
            searchBookSuspend(scope, key, page, variableBook)
        }
    }

    suspend fun searchBookSuspend(
        scope: CoroutineScope,
        key: String,
        page: Int? = 1,
        variableBook: SearchBook,
    ): ArrayList<SearchBook> {
        bookSource.searchUrl?.let { searchUrl ->
            val analyzeUrl = AnalyzeUrl(
                ruleUrl = searchUrl,
                key = key,
                page = page,
                baseUrl = sourceUrl,
                headerMapF = bookSource.getHeaderMap(),
                book = variableBook
            )
            val res = analyzeUrl.getResponseAwait(bookSource.bookSourceUrl)
            return BookList.analyzeBookList(
                scope,
                res.body,
                bookSource,
                analyzeUrl,
                res.url,
                variableBook,
                true
            )
        }
        return arrayListOf()
    }

    /**
     * 发现
     */
    fun exploreBook(
        url: String,
        page: Int? = 1,
        variableBook: SearchBook,
        scope: CoroutineScope = Coroutine.DEFAULT,
        context: CoroutineContext = Dispatchers.IO,
    ): Coroutine<List<SearchBook>> {
        return Coroutine.async(scope, context) {
            val analyzeUrl = AnalyzeUrl(
                ruleUrl = url,
                page = page,
                baseUrl = sourceUrl,
                headerMapF = bookSource.getHeaderMap()
            )
            val res = analyzeUrl.getResponseAwait(bookSource.bookSourceUrl)
            BookList.analyzeBookList(
                scope,
                res.body,
                bookSource,
                analyzeUrl,
                res.url,
                variableBook,
                false
            )
        }
    }

    /**
     * 书籍信息
     */
    fun getBookInfo(
        book: Book,
        scope: CoroutineScope = Coroutine.DEFAULT,
        context: CoroutineContext = Dispatchers.IO,
        canReName: Boolean = true,
    ): Coroutine<Book> {
        return Coroutine.async(scope, context) {
            book.type = bookSource.bookSourceType
            if (!book.infoHtml.isNullOrEmpty()) {
                book.infoHtml
                BookInfo.analyzeBookInfo(book, book.infoHtml, bookSource, book.bookUrl, canReName)
            } else {
                val res = AnalyzeUrl(
                    ruleUrl = book.bookUrl,
                    baseUrl = sourceUrl,
                    headerMapF = bookSource.getHeaderMap(),
                    book = book
                ).getResponseAwait(bookSource.bookSourceUrl)
                BookInfo.analyzeBookInfo(book, res.body, bookSource, res.url, canReName)
            }
            book
        }
    }

    /**
     * 目录
     */
    fun getChapterList(
        book: Book,
        scope: CoroutineScope = Coroutine.DEFAULT,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<List<BookChapter>> {
        return Coroutine.async(scope, context) {
            book.type = bookSource.bookSourceType
            if (book.bookUrl == book.tocUrl && !book.tocHtml.isNullOrEmpty()) {
                BookChapterList.analyzeChapterList(
                    this,
                    book,
                    book.tocHtml,
                    bookSource,
                    book.tocUrl
                )
            } else {
                val res = AnalyzeUrl(
                    book = book,
                    ruleUrl = book.tocUrl,
                    baseUrl = book.bookUrl,
                    headerMapF = bookSource.getHeaderMap()
                ).getResponseAwait(bookSource.bookSourceUrl)
                BookChapterList.analyzeChapterList(
                    this,
                    book, res.body,
                    bookSource,
                    res.url
                )
            }

        }
    }

    /**
     * 章节内容
     */
    fun getContent(
        book: Book,
        bookChapter: BookChapter,
        nextChapterUrl: String? = null,
        scope: CoroutineScope = Coroutine.DEFAULT,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<String> {
        return Coroutine.async(scope, context) {
            getContentSuspend(
                book, bookChapter, nextChapterUrl, scope
            )
        }
    }

    /**
     * 章节内容
     */
    suspend fun getContentSuspend(
        book: Book,
        bookChapter: BookChapter,
        nextChapterUrl: String? = null,
        scope: CoroutineScope = Coroutine.DEFAULT
    ): String {
        if (bookSource.getContentRule().content.isNullOrEmpty()) {
            Debug.log(sourceUrl, "⇒正文规则为空,使用章节链接:${bookChapter.url}")
            return bookChapter.url
        }
        return if (bookChapter.url == book.bookUrl && !book.tocHtml.isNullOrEmpty()) {
            BookContent.analyzeContent(
                scope,
                book.tocHtml,
                book,
                bookChapter,
                bookSource,
                bookChapter.getAbsoluteURL(),
                nextChapterUrl
            )
        } else {
            val res = AnalyzeUrl(
                ruleUrl = bookChapter.getAbsoluteURL(),
                baseUrl = book.tocUrl,
                headerMapF = bookSource.getHeaderMap(),
                book = book,
                chapter = bookChapter
            ).getResponseAwait(
                bookSource.bookSourceUrl,
                jsStr = bookSource.getContentRule().webJs,
                sourceRegex = bookSource.getContentRule().sourceRegex
            )
            BookContent.analyzeContent(
                scope,
                res.body,
                book,
                bookChapter,
                bookSource,
                res.url,
                nextChapterUrl
            )
        }
    }
}