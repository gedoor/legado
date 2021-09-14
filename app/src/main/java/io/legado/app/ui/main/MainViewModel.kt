package io.legado.app.ui.main

import android.app.Application
import androidx.lifecycle.viewModelScope
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppLog
import io.legado.app.constant.BookType
import io.legado.app.constant.EventBus
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.help.AppConfig
import io.legado.app.help.BookHelp
import io.legado.app.help.DefaultData
import io.legado.app.help.LocalConfig
import io.legado.app.model.CacheBook
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.postEvent
import io.legado.app.utils.printOnDebug
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import kotlin.math.min

class MainViewModel(application: Application) : BaseViewModel(application) {
    private var threadCount = AppConfig.threadCount
    private var upTocPool =
        Executors.newFixedThreadPool(min(threadCount, AppConst.MAX_THREAD)).asCoroutineDispatcher()
    val onUpTocBooks = CopyOnWriteArraySet<String>()
    private val waitUpTocBooks = arrayListOf<String>()
    private var upTocJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        upTocPool.close()
    }

    fun upPool() {
        threadCount = AppConfig.threadCount
        upTocPool.close()
        upTocPool = Executors
            .newFixedThreadPool(min(threadCount, AppConst.MAX_THREAD)).asCoroutineDispatcher()
    }

    fun upAllBookToc() {
        execute {
            addToWaitUp(appDb.bookDao.hasUpdateBooks)
        }
    }

    fun upToc(books: List<Book>) {
        execute(context = upTocPool) {
            books.filter {
                it.origin != BookType.local && it.canUpdate
            }.let {
                addToWaitUp(it)
            }
        }
    }

    @Synchronized
    private fun addToWaitUp(books: List<Book>) {
        books.forEach { book ->
            if (!waitUpTocBooks.contains(book.bookUrl) && !onUpTocBooks.contains(book.bookUrl)) {
                waitUpTocBooks.add(book.bookUrl)
            }
        }
        if (upTocJob == null) {
            startUpTocJob()
        }
    }

    private fun startUpTocJob() {
        upTocJob = viewModelScope.launch(upTocPool) {
            while (isActive) {
                when {
                    waitUpTocBooks.isEmpty() -> {
                        upTocJob?.cancel()
                        upTocJob = null
                    }
                    onUpTocBooks.size < threadCount -> {
                        updateToc()
                    }
                    else -> {
                        delay(500)
                    }
                }
            }
        }
    }

    @Synchronized
    private fun updateToc() {
        val bookUrl = waitUpTocBooks.firstOrNull() ?: return
        if (onUpTocBooks.contains(bookUrl)) {
            waitUpTocBooks.remove(bookUrl)
            return
        }
        val book = appDb.bookDao.getBook(bookUrl)
        if (book == null) {
            waitUpTocBooks.remove(bookUrl)
            return
        }
        val source = appDb.bookSourceDao.getBookSource(book.origin)
        if (source == null) {
            waitUpTocBooks.remove(book.bookUrl)
            return
        }
        waitUpTocBooks.remove(book.bookUrl)
        onUpTocBooks.add(book.bookUrl)
        postEvent(EventBus.UP_BOOKSHELF, book.bookUrl)
        execute(context = upTocPool) {
            if (book.tocUrl.isBlank()) {
                WebBook.getBookInfoAwait(this, source, book)
            }
            val toc = WebBook.getChapterListAwait(this, source, book)
            appDb.bookDao.update(book)
            appDb.bookChapterDao.delByBook(book.bookUrl)
            appDb.bookChapterDao.insert(*toc.toTypedArray())
            cacheBook(book)
        }.onError(upTocPool) {
            AppLog.addLog("${book.name} 更新目录失败\n${it.localizedMessage}", it)
            it.printOnDebug()
        }.onCancel(upTocPool) {
            upTocCancel(book.bookUrl)
        }.onFinally(upTocPool) {
            upTocFinally(book.bookUrl)
        }
    }

    @Synchronized
    private fun upTocCancel(bookUrl: String) {
        onUpTocBooks.remove(bookUrl)
        waitUpTocBooks.add(bookUrl)
    }

    @Synchronized
    private fun upTocFinally(bookUrl: String) {
        waitUpTocBooks.remove(bookUrl)
        onUpTocBooks.remove(bookUrl)
        postEvent(EventBus.UP_BOOKSHELF, bookUrl)
    }

    /**
     * 缓存书籍
     */
    private fun cacheBook(book: Book) {
        val endIndex = min(
            book.totalChapterNum - 1,
            book.durChapterIndex.plus(AppConfig.preDownloadNum)
        )
        for (i in book.durChapterIndex..endIndex) {
            appDb.bookChapterDao.getChapter(book.bookUrl, i)?.let { chapter ->
                if (!BookHelp.hasContent(book, chapter)) {
                    CacheBook.start(context, book.bookUrl, i, endIndex)
                    return
                }
            }
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