package io.legado.app.ui.main

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.BookType
import io.legado.app.constant.EventBus
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.AppConfig
import io.legado.app.help.BookHelp
import io.legado.app.help.DefaultData
import io.legado.app.help.LocalConfig
import io.legado.app.model.CacheBook
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.postEvent
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import kotlin.math.min

class MainViewModel(application: Application) : BaseViewModel(application) {
    private var threadCount = AppConfig.threadCount
    private var upTocPool = Executors.newFixedThreadPool(min(threadCount,8)).asCoroutineDispatcher()
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
        upTocPool = Executors.newFixedThreadPool(min(threadCount,8)).asCoroutineDispatcher()
    }

    fun upAllBookToc() {
        execute {
            upToc(appDb.bookDao.hasUpdateBooks)
        }
    }

    fun upToc(books: List<Book>) {
        execute(context = upTocPool) {
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
        var update = false
        bookMap.forEach { bookEntry ->
            if (!updateList.contains(bookEntry.key)) {
                update = true
                val book = bookEntry.value
                synchronized(this) {
                    updateList.add(book.bookUrl)
                    postEvent(EventBus.UP_BOOKSHELF, book.bookUrl)
                }
                appDb.bookSourceDao.getBookSource(book.origin)?.let { bookSource ->
                    execute(context = upTocPool) {
                        if (book.tocUrl.isBlank()) {
                            WebBook.getBookInfoAwait(this, bookSource, book)
                        }
                        val toc = WebBook.getChapterListAwait(this, bookSource, book)
                        appDb.bookDao.update(book)
                        appDb.bookChapterDao.delByBook(book.bookUrl)
                        appDb.bookChapterDao.insert(*toc.toTypedArray())
                        cacheBook(bookSource, book)
                    }.onError(upTocPool) {
                        it.printStackTrace()
                    }.onFinally(upTocPool) {
                        synchronized(this) {
                            bookMap.remove(bookEntry.key)
                            updateList.remove(book.bookUrl)
                            postEvent(EventBus.UP_BOOKSHELF, book.bookUrl)
                            upNext()
                        }
                    }
                } ?: synchronized(this) {
                    bookMap.remove(bookEntry.key)
                    updateList.remove(book.bookUrl)
                    postEvent(EventBus.UP_BOOKSHELF, book.bookUrl)
                    upNext()
                }
                return
            }
        }
        if (!update) {
            usePoolCount--
        }
    }

    private fun cacheBook(bookSource: BookSource, book: Book) {
        execute {
            if (book.totalChapterNum > book.durChapterIndex) {
                val downloadToIndex =
                    min(book.totalChapterNum, book.durChapterIndex.plus(AppConfig.preDownloadNum))
                for (i in book.durChapterIndex until downloadToIndex) {
                    appDb.bookChapterDao.getChapter(book.bookUrl, i)?.let { chapter ->
                        if (!BookHelp.hasContent(book, chapter)) {
                            var addToCache = false
                            while (!addToCache) {
                                val cacheBook = CacheBook.get(bookSource, book)
                                if (CacheBook.onDownloadCount < 10) {
                                    cacheBook.download(this, chapter)
                                    addToCache = true
                                } else {
                                    delay(100)
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
            if (appDb.httpTTSDao.count == 0) {
                DefaultData.httpTTS.let {
                    appDb.httpTTSDao.insert(*it.toTypedArray())
                }
            }
        }
    }

    fun upVersion() {
        execute {
            if (LocalConfig.needUpHttpTTS) {
                DefaultData.importDefaultHttpTTS()
            }
            if (LocalConfig.needUpTxtTocRule) {
                DefaultData.importDefaultTocRules()
            }
            if (LocalConfig.needUpRssSources) {
                DefaultData.importDefaultRssSources()
            }
        }
    }
}