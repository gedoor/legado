package io.legado.app.ui.book.arrange

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book


class ArrangeBookViewModel(application: Application) : BaseViewModel(application) {


    fun deleteBook(vararg book: Book) {
        execute {
            App.db.bookDao().delete(*book)
        }
    }

}