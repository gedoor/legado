package io.legado.app.ui.book.searchContent


import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.help.ContentProcessor

class SearchContentViewModel(application: Application) : BaseViewModel(application) {
    var bookUrl: String = ""
    var book: Book? = null
    var contentProcessor: ContentProcessor? = null
    var lastQuery: String = ""

    fun initBook(bookUrl: String, success: () -> Unit) {
        this.bookUrl = bookUrl
        execute {
            book = appDb.bookDao.getBook(bookUrl)
            book?.let {
                contentProcessor = ContentProcessor.get(it.name, it.origin)
            }
        }.onSuccess {
            success.invoke()
        }
    }

}