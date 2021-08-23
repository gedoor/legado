package io.legado.app.model.webBook

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.http.StrResponse
import io.legado.app.model.Debug
import io.legado.app.model.analyzeRule.AnalyzeUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

@Suppress("MemberVisibilityCanBePrivate")
class WebBook(val bookSource: BookSource) {

    val sourceUrl: String
        get() = bookSource.bookSourceUrl

    /**
     * 搜索
     */
    fun searchBook(
        scope: CoroutineScope,
        key: String,
        page: Int? = 1,
        context: CoroutineContext = Dispatchers.IO,
    ): Coroutine<ArrayList<SearchBook>> {
        return Coroutine.async(scope, context) {
            searchBookAwait(scope, key, page)
        }
    }

    suspend fun searchBookAwait(
        scope: CoroutineScope,
        key: String,
        page: Int? = 1,
    ): ArrayList<SearchBook> {
        val variableBook = SearchBook()
        bookSource.searchUrl?.let { searchUrl ->
            val analyzeUrl = AnalyzeUrl(
                ruleUrl = searchUrl,
                key = key,
                page = page,
                baseUrl = sourceUrl,
                headerMapF = bookSource.getHeaderMap(true),
                book = variableBook,
                source = bookSource
            )
            var res = analyzeUrl.getStrResponse(bookSource.bookSourceUrl)
            //检测书源是否已登录
            bookSource.loginCheckJs?.let { checkJs ->
                if (checkJs.isNotBlank()) {
                    res = analyzeUrl.evalJS(checkJs) as StrResponse
                }
            }
            return BookList.analyzeBookList(scope, res, bookSource, analyzeUrl, variableBook, true)
        }
        return arrayListOf()
    }

    /**
     * 发现
     */
    fun exploreBook(
        scope: CoroutineScope,
        url: String,
        page: Int? = 1,
        context: CoroutineContext = Dispatchers.IO,
    ): Coroutine<List<SearchBook>> {
        return Coroutine.async(scope, context) {
            exploreBookAwait(scope, url, page)
        }
    }

    suspend fun exploreBookAwait(
        scope: CoroutineScope,
        url: String,
        page: Int? = 1,
    ): ArrayList<SearchBook> {
        val variableBook = SearchBook()
        val analyzeUrl = AnalyzeUrl(
            ruleUrl = url,
            page = page,
            baseUrl = sourceUrl,
            book = variableBook,
            source = bookSource,
            headerMapF = bookSource.getHeaderMap(true)
        )
        var res = analyzeUrl.getStrResponse(bookSource.bookSourceUrl)
        //检测书源是否已登录
        bookSource.loginCheckJs?.let { checkJs ->
            if (checkJs.isNotBlank()) {
                res = analyzeUrl.evalJS(checkJs) as StrResponse
            }
        }
        return BookList.analyzeBookList(scope, res, bookSource, analyzeUrl, variableBook, false)
    }

    /**
     * 书籍信息
     */
    fun getBookInfo(
        scope: CoroutineScope,
        book: Book,
        context: CoroutineContext = Dispatchers.IO,
        canReName: Boolean = true,
    ): Coroutine<Book> {
        return Coroutine.async(scope, context) {
            getBookInfoAwait(scope, book, canReName)
        }
    }

    suspend fun getBookInfoAwait(
        scope: CoroutineScope,
        book: Book,
        canReName: Boolean = true,
    ): Book {
        book.type = bookSource.bookSourceType
        if (!book.infoHtml.isNullOrEmpty()) {
            val strResponse = StrResponse(book.bookUrl, book.infoHtml)
            BookInfo.analyzeBookInfo(scope, strResponse, bookSource, book, book.bookUrl, canReName)
        } else {
            val analyzeUrl = AnalyzeUrl(
                ruleUrl = book.bookUrl,
                baseUrl = sourceUrl,
                book = book,
                source = bookSource,
                headerMapF = bookSource.getHeaderMap(true)
            )
            var res = analyzeUrl.getStrResponse(bookSource.bookSourceUrl)
            //检测书源是否已登录
            bookSource.loginCheckJs?.let { checkJs ->
                if (checkJs.isNotBlank()) {
                    res = analyzeUrl.evalJS(checkJs) as StrResponse
                }
            }
            BookInfo.analyzeBookInfo(scope, res, bookSource, book, book.bookUrl, canReName)
        }
        return book
    }

    /**
     * 目录
     */
    fun getChapterList(
        scope: CoroutineScope,
        book: Book,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<List<BookChapter>> {
        return Coroutine.async(scope, context) {
            getChapterListAwait(scope, book)
        }
    }

    suspend fun getChapterListAwait(
        scope: CoroutineScope,
        book: Book,
    ): List<BookChapter> {
        book.type = bookSource.bookSourceType
        return if (book.bookUrl == book.tocUrl && !book.tocHtml.isNullOrEmpty()) {
            val strResponse = StrResponse(book.tocUrl, book.tocHtml)
            BookChapterList.analyzeChapterList(scope, strResponse, bookSource, book, book.tocUrl)
        } else {
            val analyzeUrl = AnalyzeUrl(
                ruleUrl = book.tocUrl,
                baseUrl = book.bookUrl,
                book = book,
                source = bookSource,
                headerMapF = bookSource.getHeaderMap(true)
            )
            var res = analyzeUrl.getStrResponse(bookSource.bookSourceUrl)
            //检测书源是否已登录
            bookSource.loginCheckJs?.let { checkJs ->
                if (checkJs.isNotBlank()) {
                    res = analyzeUrl.evalJS(checkJs) as StrResponse
                }
            }
            BookChapterList.analyzeChapterList(scope, res, bookSource, book, book.tocUrl)
        }
    }

    /**
     * 章节内容
     */
    fun getContent(
        scope: CoroutineScope,
        book: Book,
        bookChapter: BookChapter,
        nextChapterUrl: String? = null,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<String> {
        return Coroutine.async(scope, context) {
            getContentAwait(scope, book, bookChapter, nextChapterUrl)
        }
    }

    suspend fun getContentAwait(
        scope: CoroutineScope,
        book: Book,
        bookChapter: BookChapter,
        nextChapterUrl: String? = null,
    ): String {
        if (bookSource.getContentRule().content.isNullOrEmpty()) {
            Debug.log(sourceUrl, "⇒正文规则为空,使用章节链接:${bookChapter.url}")
            return bookChapter.url
        }
        return if (bookChapter.url == book.bookUrl && !book.tocHtml.isNullOrEmpty()) {
            val strResponse = StrResponse(bookChapter.getAbsoluteURL(), book.tocHtml)
            BookContent.analyzeContent(
                scope,
                strResponse,
                bookSource,
                book,
                bookChapter,
                bookChapter.getAbsoluteURL(),
                nextChapterUrl
            )
        } else {
            val analyzeUrl = AnalyzeUrl(
                ruleUrl = bookChapter.getAbsoluteURL(),
                baseUrl = book.tocUrl,
                book = book,
                source = bookSource,
                chapter = bookChapter,
                headerMapF = bookSource.getHeaderMap(true)
            )
            var res = analyzeUrl.getStrResponse(
                bookSource.bookSourceUrl,
                jsStr = bookSource.getContentRule().webJs,
                sourceRegex = bookSource.getContentRule().sourceRegex
            )
            //检测书源是否已登录
            bookSource.loginCheckJs?.let { checkJs ->
                if (checkJs.isNotBlank()) {
                    res = analyzeUrl.evalJS(checkJs) as StrResponse
                }
            }
            BookContent.analyzeContent(
                scope,
                res,
                bookSource,
                book,
                bookChapter,
                bookChapter.getAbsoluteURL(),
                nextChapterUrl
            )
        }
    }
}