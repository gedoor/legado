package io.legado.app.ui.main

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.BookType
import io.legado.app.constant.Bus
import io.legado.app.data.entities.RssSource
import io.legado.app.help.storage.Restore
import io.legado.app.model.WebBook
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.postEvent
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay

class MainViewModel(application: Application) : BaseViewModel(application) {
    val updateList = arrayListOf<String>()

    fun importYueDuData() {
        execute {
            Restore.importYueDuData(getApplication())
            initRssSource()
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
                }
                delay(50)
            }
        }
    }

    fun initRssSource() {
        execute {
            if (App.db.rssSourceDao().size == 0) {
                val json = String(context.assets.open("rssSource.json").readBytes())
                val sources = GSON.fromJsonArray<RssSource>(json)
                if (sources != null) {
                    App.db.rssSourceDao().insert(*sources.toTypedArray())
                }
            }
        }
    }
}