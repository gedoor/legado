package io.legado.app.ui.main

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.BookType
import io.legado.app.constant.EventBus
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.RssSource
import io.legado.app.help.AppConfig
import io.legado.app.help.http.HttpHelper
import io.legado.app.help.storage.Restore
import io.legado.app.model.WebBook
import io.legado.app.utils.FileUtils
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.postEvent
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

class MainViewModel(application: Application) : BaseViewModel(application) {
    private var upTocPool =
        Executors.newFixedThreadPool(AppConfig.threadCount).asCoroutineDispatcher()
    val updateList = hashSetOf<String>()

    override fun onCleared() {
        super.onCleared()
        upTocPool.close()
    }

    fun upPool() {
        upTocPool.close()
        upTocPool = Executors.newFixedThreadPool(AppConfig.threadCount).asCoroutineDispatcher()
    }

    fun upChapterList() {
        execute {
            upChapterList(App.db.bookDao().hasUpdateBooks)
        }
    }

    fun upChapterList(books: List<Book>) {
        execute {
            books.filter {
                it.origin != BookType.local && it.canUpdate
            }.forEach { book ->
                if (!updateList.contains(book.bookUrl)) {
                    App.db.bookSourceDao().getBookSource(book.origin)?.let { bookSource ->
                        synchronized(this) {
                            updateList.add(book.bookUrl)
                            postEvent(EventBus.UP_BOOK, book.bookUrl)
                        }
                        WebBook(bookSource).getChapterList(book, context = upTocPool)
                            .timeout(300000)
                            .onSuccess(IO) {
                                synchronized(this) {
                                    updateList.remove(book.bookUrl)
                                    postEvent(EventBus.UP_BOOK, book.bookUrl)
                                }
                                App.db.bookDao().update(book)
                                App.db.bookChapterDao().delByBook(book.bookUrl)
                                App.db.bookChapterDao().insert(*it.toTypedArray())
                            }
                            .onError {
                                synchronized(this) {
                                    updateList.remove(book.bookUrl)
                                    postEvent(EventBus.UP_BOOK, book.bookUrl)
                                }
                                it.printStackTrace()
                            }
                    }
                }
                delay(50)
            }
        }
    }

    fun initRss() {
        execute {
            val url = "https://gitee.com/alanskycn/yuedu/raw/master/JS/RSS/rssSource"
            HttpHelper.simpleGet(url)?.let { body ->
                val sources = mutableListOf<RssSource>()
                val items: List<Map<String, Any>> = Restore.jsonPath.parse(body).read("$")
                for (item in items) {
                    val jsonItem = Restore.jsonPath.parse(item)
                    GSON.fromJsonObject<RssSource>(jsonItem.jsonString())?.let { source ->
                        sources.add(source)
                    }
                }
                App.db.rssSourceDao().insert(*sources.toTypedArray())
            }
        }
    }

    fun postLoad() {
        execute {
            FileUtils.deleteFile(FileUtils.getPath(context.cacheDir, "Fonts"))
        }
    }
}