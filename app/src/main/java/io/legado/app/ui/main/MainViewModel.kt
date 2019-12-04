package io.legado.app.ui.main

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.Bus
import io.legado.app.model.WebBook
import io.legado.app.utils.postEvent
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay

class MainViewModel(application: Application) : BaseViewModel(application) {
    val updateList = arrayListOf<String>()

    fun upChapterList() {
        execute {
            App.db.bookDao().hasUpdateBooks.forEach { book ->
                if (!updateList.contains(book.bookUrl)) {
                    App.db.bookSourceDao().getBookSource(book.origin)?.let { bookSource ->
                        synchronized(this) {
                            updateList.add(book.bookUrl)
                            postEvent(Bus.UP_BOOK, book.bookUrl)
                        }
                        WebBook(bookSource).getChapterList(book)
                            .timeout(300000)
                            .onSuccess(IO) {
                                synchronized(this) {
                                    updateList.remove(book.bookUrl)
                                    postEvent(Bus.UP_BOOK, book.bookUrl)
                                }
                                it?.let {
                                    App.db.bookDao().update(book)
                                    App.db.bookChapterDao().delByBook(book.bookUrl)
                                    App.db.bookChapterDao().insert(*it.toTypedArray())
                                }
                            }
                            .onError {
                                synchronized(this) {
                                    updateList.remove(book.bookUrl)
                                    postEvent(Bus.UP_BOOK, book.bookUrl)
                                }
                                it.printStackTrace()
                            }
                    }
                }
                delay(50)
            }
        }
    }

}