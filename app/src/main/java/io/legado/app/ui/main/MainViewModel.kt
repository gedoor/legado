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
import io.legado.app.data.entities.BookSource
import io.legado.app.help.AppWebDav
import io.legado.app.help.DefaultData
import io.legado.app.help.book.*
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.LocalConfig
import io.legado.app.model.CacheBook
import io.legado.app.model.ReadBook
import io.legado.app.model.webBook.WebBook
import io.legado.app.service.CacheBookService
import io.legado.app.utils.postEvent
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import kotlin.math.min

class MainViewModel(application: Application) : BaseViewModel(application) {
    private var threadCount = AppConfig.threadCount
    private var upTocPool =
        Executors.newFixedThreadPool(min(threadCount, AppConst.MAX_THREAD)).asCoroutineDispatcher()
    private val waitUpTocBooks = arrayListOf<String>()
    private val onUpTocBooks = CopyOnWriteArraySet<String>()
    private var upTocJob: Job? = null
    private var cacheBookJob: Job? = null

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

    fun isUpdate(bookUrl: String): Boolean {
        return onUpTocBooks.contains(bookUrl)
    }

    fun upAllBookToc() {
        execute {
            addToWaitUp(appDb.bookDao.hasUpdateBooks)
        }
    }

    fun upToc(books: List<Book>) {
        execute(context = upTocPool) {
            books.filter {
                !it.isLocal && it.canUpdate
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
            if (!book.isUpError) {
                book.addType(BookType.updateError)
                appDb.bookDao.update(book)
            }
            waitUpTocBooks.remove(book.bookUrl)
            return
        }
        waitUpTocBooks.remove(bookUrl)
        upTocAdd(bookUrl)
        execute(context = upTocPool) {
            kotlin.runCatching {
                val oldBook = book.copy()
                WebBook.runPreUpdateJs(source, book)
                if (book.tocUrl.isBlank()) {
                    WebBook.getBookInfoAwait(source, book)
                }
                val toc = WebBook.getChapterListAwait(source, book).getOrThrow()
                book.sync(oldBook)
                book.removeType(BookType.updateError)
                if (book.bookUrl == bookUrl) {
                    appDb.bookDao.update(book)
                } else {
                    upTocAdd(book.bookUrl)
                    appDb.bookDao.insert(book)
                    BookHelp.updateCacheFolder(oldBook, book)
                }
                appDb.bookChapterDao.delByBook(bookUrl)
                appDb.bookChapterDao.insert(*toc.toTypedArray())
                if (book.isSameNameAuthor(ReadBook)) {
                    ReadBook.book = book
                    ReadBook.chapterSize = book.totalChapterNum
                }
                addDownload(source, book)
            }.onFailure {
                AppLog.put("${book.name} 更新目录失败\n${it.localizedMessage}", it)
                //这里可能因为时间太长书籍信息已经更改,所以重新获取
                appDb.bookDao.getBook(book.bookUrl)?.let { book ->
                    book.addType(BookType.updateError)
                    appDb.bookDao.update(book)
                }
            }
        }.onCancel(upTocPool) {
            upTocCancel(bookUrl)
            upTocCancel(book.bookUrl)
        }.onFinally(upTocPool) {
            upTocFinally(bookUrl)
            upTocFinally(book.bookUrl)
        }
    }

    @Synchronized
    private fun upTocAdd(bookUrl: String) {
        onUpTocBooks.add(bookUrl)
        postEvent(EventBus.UP_BOOKSHELF, bookUrl)
    }

    @Synchronized
    private fun upTocCancel(bookUrl: String) {
        onUpTocBooks.remove(bookUrl)
        waitUpTocBooks.add(bookUrl)
        postEvent(EventBus.UP_BOOKSHELF, bookUrl)
    }

    @Synchronized
    private fun upTocFinally(bookUrl: String) {
        waitUpTocBooks.remove(bookUrl)
        onUpTocBooks.remove(bookUrl)
        postEvent(EventBus.UP_BOOKSHELF, bookUrl)
        if (waitUpTocBooks.isEmpty()
            && onUpTocBooks.isEmpty()
            && cacheBookJob == null
            && !CacheBookService.isRun
        ) {
            //所有目录更新完再开始缓存章节
            cacheBook()
        }
    }

    @Synchronized
    private fun addDownload(source: BookSource, book: Book) {
        if (AppConfig.preDownloadNum == 0) return
        val endIndex = min(
            book.totalChapterNum - 1,
            book.durChapterIndex.plus(AppConfig.preDownloadNum)
        )
        val cacheBook = CacheBook.getOrCreate(source, book)
        cacheBook.addDownload(book.durChapterIndex, endIndex)
    }

    /**
     * 缓存书籍
     */
    private fun cacheBook() {
        cacheBookJob?.cancel()
        cacheBookJob = viewModelScope.launch(upTocPool) {
            while (isActive) {
                if (CacheBookService.isRun || !CacheBook.isRun) {
                    cacheBookJob?.cancel()
                    cacheBookJob = null
                    return@launch
                }
                CacheBook.cacheBookMap.forEach {
                    val cacheBookModel = it.value
                    while (cacheBookModel.waitCount > 0) {
                        //有目录更新是不缓存,优先更新目录,现在更多网站限制并发
                        if (waitUpTocBooks.isEmpty()
                            && onUpTocBooks.isEmpty()
                            && CacheBook.onDownloadCount < threadCount
                        ) {
                            cacheBookModel.download(this, upTocPool)
                        } else {
                            delay(100)
                        }
                    }
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

    fun restoreWebDav(name: String) {
        execute {
            AppWebDav.restoreWebDav(name)
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