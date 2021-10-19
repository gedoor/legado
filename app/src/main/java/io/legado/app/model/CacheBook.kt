package io.legado.app.model

import android.content.Context
import io.legado.app.constant.AppLog
import io.legado.app.constant.EventBus
import io.legado.app.constant.IntentAction
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookHelp
import io.legado.app.model.webBook.WebBook
import io.legado.app.service.CacheBookService
import io.legado.app.utils.postEvent

import io.legado.app.utils.startService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

object CacheBook {

    val cacheBookMap = ConcurrentHashMap<String, CacheBookModel>()

    @Synchronized
    fun getOrCreate(bookUrl: String): CacheBookModel? {
        val book = appDb.bookDao.getBook(bookUrl) ?: return null
        val bookSource = appDb.bookSourceDao.getBookSource(book.origin) ?: return null
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

    fun start(context: Context, bookUrl: String, start: Int, end: Int) {
        context.startService<CacheBookService> {
            action = IntentAction.start
            putExtra("bookUrl", bookUrl)
            putExtra("start", start)
            putExtra("end", end)
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

    val downloadSummary: String
        get() {
            return "正在下载:${onDownloadCount}|等待中:${waitCount}|失败:${errorCount}|成功:${successCount}"
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

    private val successCount: Int
        get() {
            var count = 0
            cacheBookMap.forEach {
                count += it.value.successCount
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

    private val errorCount: Int
        get() {
            var count = 0
            cacheBookMap.forEach {
                count += it.value.errorCount
            }
            return count
        }

    class CacheBookModel(var bookSource: BookSource, var book: Book) {

        private val waitDownloadSet = hashSetOf<Int>()
        private val onDownloadSet = hashSetOf<Int>()
        private val successDownloadSet = hashSetOf<Int>()
        private val errorDownloadMap = hashMapOf<Int, Int>()

        val waitCount get() = waitDownloadSet.size
        val onDownloadCount get() = onDownloadSet.size
        val successCount get() = successDownloadSet.size
        val errorCount get() = errorDownloadMap.size

        @Synchronized
        fun isRun(): Boolean {
            return waitDownloadSet.size > 0 || onDownloadSet.size > 0
        }

        @Synchronized
        fun stop() {
            waitDownloadSet.clear()
        }

        @Synchronized
        fun addDownload(start: Int, end: Int) {
            for (i in start..end) {
                if (!onDownloadSet.contains(i)) {
                    waitDownloadSet.add(i)
                }
            }
        }

        @Synchronized
        private fun onSuccess(index: Int) {
            onDownloadSet.remove(index)
            successDownloadSet.add(index)
            errorDownloadMap.remove(index)
        }

        @Synchronized
        private fun onError(index: Int, error: Throwable, chapterTitle: String) {
            if (error !is ConcurrentException) {
                errorDownloadMap[index] = (errorDownloadMap[index] ?: 0) + 1
            }
            onDownloadSet.remove(index)
            //重试3次
            if (errorDownloadMap[index] ?: 0 < 3) {
                waitDownloadSet.add(index)
            } else {
                AppLog.put(
                    "下载${book.name}-${chapterTitle}失败\n${error.localizedMessage}",
                    error
                )
                Timber.e(error)
            }
        }

        @Synchronized
        private fun onCancel(index: Int) {
            onDownloadSet.remove(index)
            waitDownloadSet.add(index)
        }

        @Synchronized
        private fun onFinally() {
            if (waitDownloadSet.isEmpty() && onDownloadSet.isEmpty()) {
                postEvent(EventBus.UP_DOWNLOAD, "")
                cacheBookMap.remove(book.bookUrl)
            }
        }

        /**
         * 从待下载列表内取第一条下载
         */
        @Synchronized
        fun download(scope: CoroutineScope, context: CoroutineContext): Boolean {
            val chapterIndex = waitDownloadSet.firstOrNull()
            if (chapterIndex == null) {
                if (onDownloadSet.isEmpty()) {
                    cacheBookMap.remove(book.bookUrl)
                }
                return false
            }
            if (onDownloadSet.contains(chapterIndex)) {
                waitDownloadSet.remove(chapterIndex)
                return download(scope, context)
            }
            val chapter = appDb.bookChapterDao.getChapter(book.bookUrl, chapterIndex) ?: let {
                waitDownloadSet.remove(chapterIndex)
                return download(scope, context)
            }
            if (BookHelp.hasContent(book, chapter)) {
                waitDownloadSet.remove(chapterIndex)
                return download(scope, context)
            }
            waitDownloadSet.remove(chapterIndex)
            onDownloadSet.add(chapterIndex)
            WebBook.getContent(
                scope,
                bookSource,
                book,
                chapter,
                context = context
            ).onSuccess { content ->
                onSuccess(chapterIndex)
                downloadFinish(chapter, content)
            }.onError {
                //出现错误等待一秒后重新加入待下载列表
                delay(1000)
                onError(chapterIndex, it, chapter.title)
                downloadFinish(chapter, "获取正文失败\n${it.localizedMessage}")
            }.onCancel {
                onCancel(chapterIndex)
            }.onFinally {
                onFinally()
            }
            return true
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
            onDownloadSet.add(chapter.index)
            waitDownloadSet.remove(chapter.index)
            WebBook.getContent(scope, bookSource, book, chapter)
                .onSuccess { content ->
                    onSuccess(chapter.index)
                    downloadFinish(chapter, content, resetPageOffset)
                }.onError {
                    onError(chapter.index, it, chapter.title)
                    downloadFinish(chapter, "获取正文失败\n${it.localizedMessage}", resetPageOffset)
                }.onCancel {
                    onCancel(chapter.index)
                }.onFinally {
                    if (waitDownloadSet.isEmpty() && onDownloadSet.isEmpty()) {
                        postEvent(EventBus.UP_DOWNLOAD, "")
                    }
                }
        }

        private fun downloadFinish(
            chapter: BookChapter,
            content: String,
            resetPageOffset: Boolean = false
        ) {
            if (ReadBook.book?.bookUrl == book.bookUrl) {
                ReadBook.contentLoadFinish(
                    book, chapter, content,
                    resetPageOffset = resetPageOffset
                )
            }
        }

    }

}