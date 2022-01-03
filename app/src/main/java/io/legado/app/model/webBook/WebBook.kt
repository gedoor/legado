package io.legado.app.model.webBook

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.http.StrResponse
import io.legado.app.model.Debug
import io.legado.app.model.NoStackTraceException
import io.legado.app.model.analyzeRule.AnalyzeUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlin.coroutines.CoroutineContext

@Suppress("MemberVisibilityCanBePrivate")
object WebBook {

    /**
     * 搜索
     */
    fun searchBook(
        scope: CoroutineScope,
        bookSource: BookSource,
        key: String,
        page: Int? = 1,
        context: CoroutineContext = Dispatchers.IO,
    ): Coroutine<ArrayList<SearchBook>> {
        return Coroutine.async(scope, context) {
            searchBookAwait(scope, bookSource, key, page)
        }
    }

    suspend fun searchBookAwait(
        scope: CoroutineScope,
        bookSource: BookSource,
        key: String,
        page: Int? = 1,
    ): ArrayList<SearchBook> {
        val variableBook = SearchBook()
        bookSource.searchUrl?.let { searchUrl ->
            val analyzeUrl = AnalyzeUrl(
                mUrl = searchUrl,
                key = key,
                page = page,
                baseUrl = bookSource.bookSourceUrl,
                headerMapF = bookSource.getHeaderMap(true),
                source = bookSource,
                ruleData = variableBook,
            )
            var res = analyzeUrl.getStrResponseAwait()
            //检测书源是否已登录
            bookSource.loginCheckJs?.let { checkJs ->
                if (checkJs.isNotBlank()) {
                    res = analyzeUrl.evalJS(checkJs, res) as StrResponse
                }
            }
            return BookList.analyzeBookList(
                scope,
                bookSource,
                variableBook,
                analyzeUrl,
                res.url,
                res.body,
                true
            )
        }
        return arrayListOf()
    }

    /**
     * 发现
     */
    fun exploreBook(
        scope: CoroutineScope,
        bookSource: BookSource,
        url: String,
        page: Int? = 1,
        context: CoroutineContext = Dispatchers.IO,
    ): Coroutine<List<SearchBook>> {
        return Coroutine.async(scope, context) {
            exploreBookAwait(scope, bookSource, url, page)
        }
    }

    suspend fun exploreBookAwait(
        scope: CoroutineScope,
        bookSource: BookSource,
        url: String,
        page: Int? = 1,
    ): ArrayList<SearchBook> {
        val variableBook = SearchBook()
        val analyzeUrl = AnalyzeUrl(
            mUrl = url,
            page = page,
            baseUrl = bookSource.bookSourceUrl,
            source = bookSource,
            ruleData = variableBook,
            headerMapF = bookSource.getHeaderMap(true)
        )
        var res = analyzeUrl.getStrResponseAwait()
        //检测书源是否已登录
        bookSource.loginCheckJs?.let { checkJs ->
            if (checkJs.isNotBlank()) {
                res = analyzeUrl.evalJS(checkJs, result = res) as StrResponse
            }
        }
        return BookList.analyzeBookList(
            scope,
            bookSource,
            variableBook,
            analyzeUrl,
            res.url,
            res.body,
            false
        )
    }

    /**
     * 书籍信息
     */
    fun getBookInfo(
        scope: CoroutineScope,
        bookSource: BookSource,
        book: Book,
        context: CoroutineContext = Dispatchers.IO,
        canReName: Boolean = true,
    ): Coroutine<Book> {
        return Coroutine.async(scope, context) {
            getBookInfoAwait(scope, bookSource, book, canReName)
        }
    }

    suspend fun getBookInfoAwait(
        scope: CoroutineScope,
        bookSource: BookSource,
        book: Book,
        canReName: Boolean = true,
    ): Book {
        book.type = bookSource.bookSourceType
        if (!book.infoHtml.isNullOrEmpty()) {
            BookInfo.analyzeBookInfo(
                scope,
                bookSource,
                book,
                book.bookUrl,
                book.bookUrl,
                book.infoHtml,
                canReName
            )
        } else {
            val analyzeUrl = AnalyzeUrl(
                mUrl = book.bookUrl,
                baseUrl = bookSource.bookSourceUrl,
                source = bookSource,
                ruleData = book,
                headerMapF = bookSource.getHeaderMap(true)
            )
            var res = analyzeUrl.getStrResponseAwait()
            //检测书源是否已登录
            bookSource.loginCheckJs?.let { checkJs ->
                if (checkJs.isNotBlank()) {
                    res = analyzeUrl.evalJS(checkJs, result = res) as StrResponse
                }
            }
            BookInfo.analyzeBookInfo(
                scope,
                bookSource,
                book,
                book.bookUrl,
                res.url,
                res.body,
                canReName
            )
        }
        return book
    }

    /**
     * 目录
     */
    fun getChapterList(
        scope: CoroutineScope,
        bookSource: BookSource,
        book: Book,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<List<BookChapter>> {
        return Coroutine.async(scope, context) {
            getChapterListAwait(scope, bookSource, book)
        }
    }

    suspend fun getChapterListAwait(
        scope: CoroutineScope,
        bookSource: BookSource,
        book: Book,
    ): List<BookChapter> {
        book.type = bookSource.bookSourceType
        return if (book.bookUrl == book.tocUrl && !book.tocHtml.isNullOrEmpty()) {
            BookChapterList.analyzeChapterList(
                scope,
                bookSource,
                book,
                book.tocUrl,
                book.tocUrl,
                book.tocHtml
            )
        } else {
            val analyzeUrl = AnalyzeUrl(
                mUrl = book.tocUrl,
                baseUrl = book.bookUrl,
                source = bookSource,
                ruleData = book,
                headerMapF = bookSource.getHeaderMap(true)
            )
            var res = analyzeUrl.getStrResponseAwait()
            //检测书源是否已登录
            bookSource.loginCheckJs?.let { checkJs ->
                if (checkJs.isNotBlank()) {
                    res = analyzeUrl.evalJS(checkJs, result = res) as StrResponse
                }
            }
            BookChapterList.analyzeChapterList(
                scope,
                bookSource,
                book,
                book.tocUrl,
                res.url,
                res.body
            )
        }
    }

    /**
     * 章节内容
     */
    fun getContent(
        scope: CoroutineScope,
        bookSource: BookSource,
        book: Book,
        bookChapter: BookChapter,
        nextChapterUrl: String? = null,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<String> {
        return Coroutine.async(scope, context) {
            getContentAwait(scope, bookSource, book, bookChapter, nextChapterUrl)
        }
    }

    suspend fun getContentAwait(
        scope: CoroutineScope,
        bookSource: BookSource,
        book: Book,
        bookChapter: BookChapter,
        nextChapterUrl: String? = null
    ): String {
        if (bookSource.getContentRule().content.isNullOrEmpty()) {
            Debug.log(bookSource.bookSourceUrl, "⇒正文规则为空,使用章节链接:${bookChapter.url}")
            return bookChapter.url
        }
        return if (bookChapter.url == book.bookUrl && !book.tocHtml.isNullOrEmpty()) {
            BookContent.analyzeContent(
                scope,
                bookSource,
                book,
                bookChapter,
                bookChapter.getAbsoluteURL(),
                bookChapter.getAbsoluteURL(),
                book.tocHtml,
                nextChapterUrl
            )
        } else {
            val analyzeUrl = AnalyzeUrl(
                mUrl = bookChapter.getAbsoluteURL(),
                baseUrl = book.tocUrl,
                source = bookSource,
                ruleData = book,
                chapter = bookChapter,
                headerMapF = bookSource.getHeaderMap(true)
            )
            var res = analyzeUrl.getStrResponseAwait(
                jsStr = bookSource.getContentRule().webJs,
                sourceRegex = bookSource.getContentRule().sourceRegex
            )
            //检测书源是否已登录
            bookSource.loginCheckJs?.let { checkJs ->
                if (checkJs.isNotBlank()) {
                    res = analyzeUrl.evalJS(checkJs, result = res) as StrResponse
                }
            }
            BookContent.analyzeContent(
                scope,
                bookSource,
                book,
                bookChapter,
                bookChapter.getAbsoluteURL(),
                res.url,
                res.body,
                nextChapterUrl
            )
        }
    }

    /**
     * 精准搜索
     */
    fun preciseSearch(
        scope: CoroutineScope,
        bookSources: List<BookSource>,
        name: String,
        author: String,
        context: CoroutineContext = Dispatchers.IO,
    ): Coroutine<Pair<BookSource, Book>> {
        return Coroutine.async(scope, context) {
            preciseSearchAwait(scope, bookSources, name, author)
                ?: throw NoStackTraceException("没有搜索到<$name>$author")
        }
    }

    suspend fun preciseSearchAwait(
        scope: CoroutineScope,
        bookSources: List<BookSource>,
        name: String,
        author: String
    ): Pair<BookSource, Book>? {
        bookSources.forEach { source ->
            kotlin.runCatching {
                if (!scope.isActive) return null
                searchBookAwait(scope, source, name).firstOrNull {
                    it.name == name && it.author == author
                }?.let { searchBook ->
                    if (!scope.isActive) return null
                    var book = searchBook.toBook()
                    if (book.tocUrl.isBlank()) {
                        book = getBookInfoAwait(scope, source, book)
                    }
                    return Pair(source, book)
                }
            }
        }
        return null
    }

}