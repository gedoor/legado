package io.legado.app.model

import android.content.Context
import io.legado.app.constant.AppLog
import io.legado.app.constant.EventBus
import io.legado.app.constant.IntentAction
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.exception.ConcurrentException
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.isLocal
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.webBook.WebBook
import io.legado.app.service.CacheBookService
import io.legado.app.utils.postEvent
import io.legado.app.utils.startService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

object CacheBook {

    val cacheBookMap = ConcurrentHashMap<String, CacheBookModel>()

    @Synchronized
    fun getOrCreate(bookUrl: String): CacheBookModel? {
        val book = appDb.bookDao.getBook(bookUrl) ?: return null
        val bookSource = appDb.bookSourceDao.getBookSource(book.origin) ?: return null
        updateBookSource(bookSource)
        var cacheBook = cacheBookMap[bookUrl]
        if (cacheBook != null) {
            //存在时更新,书源可能会变化,必须更新
            cacheBook.bookSource = bookSource
            cacheBook.book = book
            return cacheBook
        }
        cacheBook = CacheBookModel(bookSource, book)
        cacheBookMap[bookUrl] = cacheBook
        return cacheBook
    }

    @Synchronized
    fun getOrCreate(bookSource: BookSource, book: Book): CacheBookModel {
        updateBookSource(bookSource)
        var cacheBook = cacheBookMap[book.bookUrl]
        if (cacheBook != null) {
            //存在时更新,书源可能会变化,必须更新
            cacheBook.bookSource = bookSource
            cacheBook.book = book
            return cacheBook
        }
        cacheBook = CacheBookModel(bookSource, book)
        cacheBookMap[book.bookUrl] = cacheBook
        return cacheBook
    }

    private fun updateBookSource(newBookSource: BookSource) {
        cacheBookMap.forEach {
            val model = it.value
            if (model.bookSource.bookSourceUrl == newBookSource.bookSourceUrl) {
                model.bookSource = newBookSource
            }
        }
    }

    fun start(context: Context, book: Book, start: Int, end: Int) {
        if (!book.isLocal) {
            context.startService<CacheBookService> {
                action = IntentAction.start
                putExtra("bookUrl", book.bookUrl)
                putExtra("start", start)
                putExtra("end", end)
            }
        }
    }

    fun remove(context: Context, bookUrl: String) {
        context.startService<CacheBookService> {
            action = IntentAction.remove
            putExtra("bookUrl", bookUrl)
        }
    }

    fun stop(context: Context) {
        context.startService<CacheBookService> {
            action = IntentAction.stop
        }
    }

    fun clear() {
        successDownloadSet.clear()
        errorDownloadMap.clear()
    }

    fun close() {
        cacheBookMap.forEach { it.value.stop() }
        cacheBookMap.clear()
        clear()
    }

    val downloadSummary: String
        get() {
            return "正在下载:${onDownloadCount}|等待中:${waitCount}|失败:${errorDownloadMap.count()}|成功:${successDownloadSet.size}"
        }

    val isRun: Boolean
        get() {
            var isRun = false
            cacheBookMap.forEach {
                isRun = isRun || it.value.isRun()
            }
            return isRun
        }

    private val waitCount: Int
        get() {
            var count = 0
            cacheBookMap.forEach {
                count += it.value.waitCount
            }
            return count
        }

    val onDownloadCount: Int
        get() {
            var count = 0
            cacheBookMap.forEach {
                count += it.value.onDownloadCount
            }
            return count
        }

    val successDownloadSet = linkedSetOf<String>()
    val errorDownloadMap = hashMapOf<String, Int>()

    class CacheBookModel(var bookSource: BookSource, var book: Book) {

        private val waitDownloadSet = linkedSetOf<Int>()
        private val onDownloadSet = linkedSetOf<Int>()
        private var isStopped = false
        private var waitingRetry = false

        val waitCount get() = waitDownloadSet.size
        val onDownloadCount get() = onDownloadSet.size

        init {
            postEvent(EventBus.UP_DOWNLOAD, book.bookUrl)
        }

        @Synchronized
        fun isRun(): Boolean {
            return waitDownloadSet.size > 0 || onDownloadSet.size > 0
        }

        @Synchronized
        fun isStop(): Boolean {
            return isStopped || (!isRun() && !waitingRetry)
        }

        @Synchronized
        fun stop() {
            waitDownloadSet.clear()
            isStopped = true
            postEvent(EventBus.UP_DOWNLOAD, book.bookUrl)
        }

        @Synchronized
        fun addDownload(start: Int, end: Int) {
            isStopped = false
            for (i in start..end) {
                if (!onDownloadSet.contains(i)) {
                    waitDownloadSet.add(i)
                }
            }
            cacheBookMap[book.bookUrl] = this
            postEvent(EventBus.UP_DOWNLOAD, book.bookUrl)
        }

        @Synchronized
        private fun onSuccess(chapter: BookChapter) {
            onDownloadSet.remove(chapter.index)
            successDownloadSet.add(chapter.primaryStr())
            errorDownloadMap.remove(chapter.primaryStr())
        }

        @Synchronized
        private fun onPreError(chapter: BookChapter, error: Throwable) {
            waitingRetry = true
            if (error !is ConcurrentException) {
                errorDownloadMap[chapter.primaryStr()] =
                    (errorDownloadMap[chapter.primaryStr()] ?: 0) + 1
            }
            onDownloadSet.remove(chapter.index)
        }

        @Synchronized
        private fun onPostError(chapter: BookChapter, error: Throwable) {
            //重试3次
            if ((errorDownloadMap[chapter.primaryStr()] ?: 0) < 3 && !isStopped) {
                waitDownloadSet.add(chapter.index)
            } else {
                AppLog.put(
                    "下载${book.name}-${chapter.title}失败\n${error.localizedMessage}",
                    error
                )
            }
            waitingRetry = false
        }

        @Synchronized
        private fun onError(chapter: BookChapter, error: Throwable) {
            onPreError(chapter, error)
            onPostError(chapter, error)
        }

        @Synchronized
        private fun onCancel(index: Int) {
            onDownloadSet.remove(index)
            if (!isStopped) waitDownloadSet.add(index)
        }

        @Synchronized
        private fun onFinally() {
            if (waitDownloadSet.isEmpty() && onDownloadSet.isEmpty()) {
                cacheBookMap.remove(book.bookUrl)
            }
            postEvent(EventBus.UP_DOWNLOAD, book.bookUrl)
        }

        /**
         * 从待下载列表内取第一条下载
         */
        @Synchronized
        fun download(scope: CoroutineScope, context: CoroutineContext) {
            postEvent(EventBus.UP_DOWNLOAD, book.bookUrl)
            val chapterIndex = waitDownloadSet.firstOrNull()
            if (chapterIndex == null) {
                if (onDownloadSet.isEmpty()) {
                    cacheBookMap.remove(book.bookUrl)
                }
                return
            }
            if (onDownloadSet.contains(chapterIndex)) {
                waitDownloadSet.remove(chapterIndex)
                return
            }
            val chapter = appDb.bookChapterDao.getChapter(book.bookUrl, chapterIndex) ?: let {
                waitDownloadSet.remove(chapterIndex)
                return
            }
            if (chapter.isVolume) {
                /** 修正下载计数 */
                postEvent(EventBus.SAVE_CONTENT, Pair(book, chapter))
                waitDownloadSet.remove(chapterIndex)
                return
            }
            if (BookHelp.hasImageContent(book, chapter)) {
                waitDownloadSet.remove(chapterIndex)
                return
            }
            waitDownloadSet.remove(chapterIndex)
            onDownloadSet.add(chapterIndex)
            if (BookHelp.hasContent(book, chapter)) {
                Coroutine.async(executeContext = context) {
                    BookHelp.getContent(book, chapter)?.let {
                        BookHelp.saveImages(bookSource, book, chapter, it)
                    }
                }.onSuccess {
                    onSuccess(chapter)
                }.onError {
                    onPreError(chapter, it)
                    //出现错误等待一秒后重新加入待下载列表
                    delay(1000)
                    onPostError(chapter, it)
                }.onCancel {
                    onCancel(chapterIndex)
                }.onFinally {
                    onFinally()
                }
                return
            }
            WebBook.getContent(
                scope,
                bookSource,
                book,
                chapter,
                context = context,
                start = CoroutineStart.LAZY,
                executeContext = context
            ).onSuccess { content ->
                onSuccess(chapter)
                downloadFinish(chapter, content)
            }.onError {
                onPreError(chapter, it)
                //出现错误等待一秒后重新加入待下载列表
                delay(1000)
                onPostError(chapter, it)
                downloadFinish(chapter, "获取正文失败\n${it.localizedMessage}")
            }.onCancel {
                onCancel(chapterIndex)
            }.onFinally {
                onFinally()
            }.start()
        }

        @Synchronized
        fun download(
            scope: CoroutineScope,
            chapter: BookChapter,
            resetPageOffset: Boolean = false
        ) {
            if (onDownloadSet.contains(chapter.index)) {
                return
            }
            postEvent(EventBus.UP_DOWNLOAD, book.bookUrl)
            onDownloadSet.add(chapter.index)
            waitDownloadSet.remove(chapter.index)
            WebBook.getContent(
                scope,
                bookSource,
                book,
                chapter,
                start = CoroutineStart.LAZY,
                executeContext = IO
            ).onSuccess { content ->
                onSuccess(chapter)
                ReadBook.downloadedChapters.add(chapter.index)
                ReadBook.downloadFailChapters.remove(chapter.index)
                downloadFinish(chapter, content, resetPageOffset)
            }.onError {
                onError(chapter, it)
                ReadBook.downloadFailChapters[chapter.index] =
                    (ReadBook.downloadFailChapters[chapter.index] ?: 0) + 1
                downloadFinish(chapter, "获取正文失败\n${it.localizedMessage}", resetPageOffset)
            }.onCancel {
                onCancel(chapter.index)
            }.onFinally {
                postEvent(EventBus.UP_DOWNLOAD, book.bookUrl)
            }.start()
        }

        private fun downloadFinish(
            chapter: BookChapter,
            content: String,
            resetPageOffset: Boolean = false
        ) {
            if (ReadBook.book?.bookUrl == book.bookUrl) {
                ReadBook.contentLoadFinish(
                    book, chapter, content,
                    resetPageOffset = resetPageOffset,
                )
            }
        }

    }

}