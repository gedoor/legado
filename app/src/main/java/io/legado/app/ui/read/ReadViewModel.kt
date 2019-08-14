package io.legado.app.ui.read

import android.app.Application
import android.content.Intent
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.model.WebBook

class ReadViewModel(application: Application) : BaseViewModel(application) {

    var book: Book? = null
    var bookSource: BookSource? = null

    fun initData(intent: Intent) {
        val bookUrl = intent.getStringExtra("bookUrl")
        if (!bookUrl.isNullOrEmpty()) {
            execute {
                book = App.db.bookDao().getBook(bookUrl)
                book?.let { book ->
                    if (App.db.bookChapterDao().getChapterCount(bookUrl) == 0) {
                        App.db.bookSourceDao().getBookSource(book.origin)?.let {
                            WebBook(it).getChapterList(book)
                                .onSuccess { cList ->
                                    cList?.let {
                                        App.db.bookChapterDao().insert(*cList.toTypedArray())
                                    }
                                }
                        } ?: let {

                        }
                    }
                }

            }
        }
    }

}