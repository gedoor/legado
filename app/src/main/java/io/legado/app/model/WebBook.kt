package io.legado.app.model

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.webbook.BookChapterList
import io.legado.app.model.webbook.BookContent
import io.legado.app.model.webbook.BookInfo
import io.legado.app.model.webbook.BookList
import kotlinx.coroutines.CoroutineScope

class WebBook(private val bookSource: BookSource) {

    val sourceUrl: String
        get() = bookSource.bookSourceUrl

    fun searchBook(
        key: String,
        page: Int? = 1,
        isSearch: Boolean = true,
        scope: CoroutineScope = Coroutine.DEFAULT
    ): Coroutine<List<SearchBook>> {
        return Coroutine.async(scope) {
            bookSource.getSearchRule().searchUrl?.let { searchUrl ->
                val analyzeUrl = AnalyzeUrl(searchUrl, key, page, baseUrl = sourceUrl)
                val response = analyzeUrl.getResponseAsync().await()
                BookList.analyzeBookList(response, bookSource, analyzeUrl, isSearch)
            } ?: arrayListOf()
        }
    }

    fun getBookInfo(book: Book, scope: CoroutineScope = Coroutine.DEFAULT): Coroutine<Book> {
        return Coroutine.async(scope) {
            val analyzeUrl = AnalyzeUrl(book = book, ruleUrl = book.bookUrl, baseUrl = sourceUrl)
            val response = analyzeUrl.getResponseAsync().await()
            BookInfo.analyzeBookInfo(book, response.body(), bookSource, analyzeUrl)
            book
        }
    }

    fun getChapterList(book: Book, scope: CoroutineScope = Coroutine.DEFAULT): Coroutine<List<BookChapter>> {
        return Coroutine.async(scope) {
            val analyzeUrl = AnalyzeUrl(book = book, ruleUrl = book.tocUrl, baseUrl = book.bookUrl)
            val response = analyzeUrl.getResponseAsync().await()
            BookChapterList.analyzeChapterList(this, book, response, bookSource, analyzeUrl)
        }
    }

    fun getContent(
        book: Book,
        bookChapter: BookChapter,
        nextChapterUrl: String? = null,
        scope: CoroutineScope = Coroutine.DEFAULT
    ): Coroutine<String> {
        return Coroutine.async(scope) {
            val analyzeUrl = AnalyzeUrl(book = book, ruleUrl = bookChapter.url, baseUrl = book.tocUrl)
            val response = analyzeUrl.getResponseAsync().await()
            BookContent.analyzeContent(this, response, book, bookChapter, bookSource, nextChapterUrl)
        }
    }
}