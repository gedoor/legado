package io.legado.app.ui.main

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.BookType
import io.legado.app.constant.Bus
import io.legado.app.help.storage.Restore
import io.legado.app.model.WebBook
import io.legado.app.utils.postEvent
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : BaseViewModel(application) {
    val updateList = arrayListOf<String>()

    fun restore() {
        launch(IO) {
            Restore.importYueDuData(getApplication())
        }
    }

    fun upChapterList() {
        execute {
            App.db.bookDao().allBooks.forEach { book ->
                if (book.origin != BookType.local) {
                    if (!updateList.contains(book.bookUrl)) {
                        App.db.bookSourceDao().getBookSource(book.origin)?.let { bookSource ->
                            synchronized(this) {
                                updateList.add(book.bookUrl)
                                postEvent(Bus.UP_BOOK, book.bookUrl)
                            }
                            WebBook(bookSource).getChapterList(book)
                                .onSuccess(IO) {
                                    it?.let {
                                        App.db.bookDao().update(book)
                                        App.db.bookChapterDao().delByBook(book.bookUrl)
                                        App.db.bookChapterDao().insert(*it.toTypedArray())
                                    }
                                }
                                .onFinally {
                                    synchronized(this) {
                                        updateList.remove(book.bookUrl)
                                        postEvent(Bus.UP_BOOK, book.bookUrl)
                                    }
                                }
                        }
                    }
                }
                delay(50)
            }
        }
    }
}