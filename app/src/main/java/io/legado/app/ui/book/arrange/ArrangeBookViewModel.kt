package io.legado.app.ui.book.arrange

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book


class ArrangeBookViewModel(application: Application) : BaseViewModel(application) {

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

}