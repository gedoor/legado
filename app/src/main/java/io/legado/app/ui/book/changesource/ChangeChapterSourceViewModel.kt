package io.legado.app.ui.book.changesource

import android.app.Application
import android.os.Bundle
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.SearchBook
import io.legado.app.exception.NoStackTraceException
import io.legado.app.model.webBook.WebBook
import java.util.concurrent.ConcurrentHashMap

@Suppress("MemberVisibilityCanBePrivate")
class ChangeChapterSourceViewModel(application: Application) :
    ChangeBookSourceViewModel(application) {

    var chapterIndex: Int = 0
    var chapterTitle: String = ""

    private val tocMap = ConcurrentHashMap<String, List<BookChapter>>()

    override fun initData(arguments: Bundle?) {
        super.initData(arguments)
        arguments?.let { bundle ->
            bundle.getString("chapterTitle")?.let {
                chapterTitle = it
            }
            chapterIndex = bundle.getInt("chapterIndex")
        }
    }

    fun getToc(
        searchBook: SearchBook,
        success: (toc: List<BookChapter>) -> Unit,
        error: (msg: String) -> Unit
    ) {
        execute {
            return@execute tocMap[searchBook.bookUrl]
                ?: let {
                    val book = searchBook.toBook()
                    val source = appDb.bookSourceDao.getBookSource(book.origin)
                        ?: throw NoStackTraceException("书源不存在")
                    if (book.tocUrl.isEmpty()) {
                        WebBook.getBookInfoAwait(this, source, book)
                    }
                    val toc = WebBook.getChapterListAwait(this, source, book)
                    tocMap[book.bookUrl] = toc
                    toc
                }
        }.onSuccess {
            success(it)
        }.onError {
            error(it.localizedMessage ?: "获取目录出错")
        }
    }

    fun getContent(
        book: Book,
        chapter: BookChapter,
        nextChapterUrl: String?,
        success: (content: String) -> Unit,
        error: (msg: String) -> Unit
    ) {
        execute {
            val bookSource = appDb.bookSourceDao.getBookSource(book.origin)
                ?: throw NoStackTraceException("书源不存在")
            WebBook.getContentAwait(this, bookSource, book, chapter, nextChapterUrl, false)
        }.onSuccess {
            success.invoke(it)
        }.onError {
            error.invoke(it.localizedMessage ?: "获取正文出错")
        }
    }

}