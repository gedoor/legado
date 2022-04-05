package io.legado.app.ui.book.arrange

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.webBook.WebBook


class ArrangeBookViewModel(application: Application) : BaseViewModel(application) {

    val batchChangeSourceState = mutableStateOf(false)
    val batchChangeSourceSize = mutableStateOf(0)
    val batchChangeSourcePosition = mutableStateOf(0)
    var batchChangeSourceCoroutine: Coroutine<Unit>? = null

    fun upCanUpdate(books: Array<Book>, canUpdate: Boolean) {
        execute {
            books.forEach {
                it.canUpdate = canUpdate
            }
            appDb.bookDao.update(*books)
        }
    }

    fun updateBook(vararg book: Book) {
        execute {
            appDb.bookDao.update(*book)
        }
    }

    fun deleteBook(vararg book: Book) {
        execute {
            appDb.bookDao.delete(*book)
        }
    }

    fun changeSource(books: Array<Book>, source: BookSource) {
        batchChangeSourceCoroutine?.cancel()
        batchChangeSourceCoroutine = execute {
            batchChangeSourceSize.value = books.size
            books.forEachIndexed { index, book ->
                batchChangeSourcePosition.value = index + 1
                if (book.isLocalBook()) return@forEachIndexed
                if (book.origin == source.bookSourceUrl) return@forEachIndexed
                WebBook.preciseSearchAwait(this, source, book.name, book.author)
                    .getOrNull()?.let { newBook ->
                        val toc = WebBook.getChapterListAwait(this, source, newBook)
                        book.changeTo(newBook, toc)
                        appDb.bookChapterDao.insert(*toc.toTypedArray())
                    }
            }
        }.onFinally {
            batchChangeSourceState.value = false
        }
    }

}