package io.legado.app.ui.main

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.BookType
import io.legado.app.constant.EventBus
import io.legado.app.data.entities.Book
import io.legado.app.help.AppConfig
import io.legado.app.help.BookHelp
import io.legado.app.help.DefaultData
import io.legado.app.help.LocalConfig
import io.legado.app.model.webBook.WebBook
import io.legado.app.service.help.CacheBook
import io.legado.app.utils.FileUtils
import io.legado.app.utils.postEvent
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import kotlin.math.min

class MainViewModel(application: Application) : BaseViewModel(application) {
    private var threadCount = AppConfig.threadCount
    private var upTocPool = Executors.newFixedThreadPool(threadCount).asCoroutineDispatcher()
    val updateList = CopyOnWriteArraySet<String>()
    private val bookMap = ConcurrentHashMap<String, Book>()

    @Volatile
    private var usePoolCount = 0

    override fun onCleared() {
        super.onCleared()
        upTocPool.close()
    }

    fun upPool() {
        threadCount = AppConfig.threadCount
        upTocPool.close()
        upTocPool = Executors.newFixedThreadPool(threadCount).asCoroutineDispatcher()
    }

    fun upAllBookToc() {
        execute {
            upToc(App.db.bookDao.hasUpdateBooks)
        }
    }

    fun upToc(books: List<Book>) {
        execute {
            books.filter {
                it.origin != BookType.local && it.canUpdate
            }.forEach {
                bookMap[it.bookUrl] = it
            }
            for (i in 0 until threadCount) {
                if (usePoolCount < threadCount) {
                    usePoolCount++
                    updateToc()
                }
            }
        }
    }

    @Synchronized
    private fun updateToc() {
        bookMap.forEach { bookEntry ->
            if (!updateList.contains(bookEntry.key)) {
                val book = bookEntry.value
                synchronized(this) {
                    updateList.add(book.bookUrl)
                    postEvent(EventBus.UP_BOOK, book.bookUrl)
                }
                App.db.bookSourceDao.getBookSource(book.origin)?.let { bookSource ->
                    val webBook = WebBook(bookSource)
                    webBook.getChapterList(this, book, context = upTocPool)
                        .timeout(300000)
                        .onSuccess(IO) {
                            App.db.bookDao.update(book)
                            App.db.bookChapterDao.delByBook(book.bookUrl)
                            App.db.bookChapterDao.insert(*it.toTypedArray())
                            if (AppConfig.preDownload) {
                                cacheBook(webBook, book)
                            }
                        }
                        .onError {
                            it.printStackTrace()
                        }
                        .onFinally {
                            synchronized(this) {
                                bookMap.remove(bookEntry.key)
                                updateList.remove(book.bookUrl)
                                postEvent(EventBus.UP_BOOK, book.bookUrl)
                                upNext()
                            }
                        }
                } ?: synchronized(this) {
                    bookMap.remove(bookEntry.key)
                    updateList.remove(book.bookUrl)
                    postEvent(EventBus.UP_BOOK, book.bookUrl)
                    upNext()
                }
                return
            }
        }
    }

    private fun cacheBook(webBook: WebBook, book: Book) {
        execute {
            if (book.totalChapterNum > book.durChapterIndex) {
                val downloadToIndex = min(book.totalChapterNum, book.durChapterIndex.plus(10))
                for (i in book.durChapterIndex until downloadToIndex) {
                    App.db.bookChapterDao.getChapter(book.bookUrl, i)?.let { chapter ->
                        if (!BookHelp.hasContent(book, chapter)) {
                            var addToCache = false
                            while (!addToCache) {
                                if (CacheBook.downloadCount() < 5) {
                                    CacheBook.download(this, webBook, book, chapter)
                                    addToCache = true
                                } else {
                                    delay(1000)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun upNext() {
        if (bookMap.size > updateList.size) {
            updateToc()
        } else {
            usePoolCount--
        }
    }

    fun postLoad() {
        execute {
            FileUtils.deleteFile(FileUtils.getPath(context.cacheDir, "Fonts"))
            if (App.db.httpTTSDao.count == 0) {
                DefaultData.httpTTS.let {
                    App.db.httpTTSDao.insert(*it.toTypedArray())
                }
            }
        }
    }

    fun upVersion() {
        execute {
            if (LocalConfig.hasUpHttpTTS) {
                DefaultData.importDefaultHttpTTS()
            }
            if (LocalConfig.hasUpTxtTocRule) {
                DefaultData.importDefaultTocRules()
            }
        }
    }
}