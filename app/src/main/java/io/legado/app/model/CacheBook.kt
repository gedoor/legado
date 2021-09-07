package io.legado.app.model

import android.annotation.SuppressLint
import android.content.Context
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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

class CacheBook(var bookSource: BookSource, var book: Book) {

    companion object {

        val logs = arrayListOf<String>()
        val cacheBookMap = ConcurrentHashMap<String, CacheBook>()

        @SuppressLint("ConstantLocale")
        private val logTimeFormat = SimpleDateFormat("[mm:ss.SSS]", Locale.getDefault())

        @Synchronized
        fun getOrCreate(bookUrl: String): CacheBook? {
            val book = appDb.bookDao.getBook(bookUrl) ?: return null
            val bookSource = appDb.bookSourceDao.getBookSource(book.origin) ?: return null
            var cacheBook = cacheBookMap[bookUrl]
            if (cacheBook != null) {
                //存在时更新,书源可能会变化,必须更新
                cacheBook.bookSource = bookSource
                cacheBook.book = book
                return cacheBook
            }
            cacheBook = CacheBook(bookSource, book)
            cacheBookMap[bookUrl] = cacheBook
            return cacheBook
        }

        @Synchronized
        fun getOrCreate(bookSource: BookSource, book: Book): CacheBook {
            var cacheBook = cacheBookMap[book.bookUrl]
            if (cacheBook != null) {
                //存在时更新,书源可能会变化,必须更新
                cacheBook.bookSource = bookSource
                cacheBook.book = book
                return cacheBook
            }
            cacheBook = CacheBook(bookSource, book)
            cacheBookMap[book.bookUrl] = cacheBook
            return cacheBook
        }

        fun addLog(log: String?) {
            log ?: return
            synchronized(this) {
                if (logs.size > 1000) {
                    logs.removeAt(0)
                }
                logs.add(logTimeFormat.format(Date()) + " " + log)
            }
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

        val isRun: Boolean get() = waitDownloadCount > 0 || onDownloadCount > 0

        val waitDownloadCount: Int
            get() {
                var count = 0
                cacheBookMap.forEach {
                    count += it.value.waitDownloadSet.size
                }
                return count
            }

        val successDownloadCount: Int
            get() {
                var count = 0
                cacheBookMap.forEach {
                    count += it.value.successDownloadSet.size
                }
                return count
            }

        val onDownloadCount: Int
            get() {
                var count = 0
                cacheBookMap.forEach {
                    count += it.value.onDownloadSet.size
                }
                return count
            }

    }

    val waitDownloadSet = hashSetOf<Int>()
    val onDownloadSet = hashSetOf<Int>()
    val successDownloadSet = hashSetOf<Int>()

    fun addDownload(start: Int, end: Int) {
        synchronized(this) {
            for (i in start..end) {
                waitDownloadSet.add(i)
            }
        }
    }

    fun isRun(): Boolean {
        return synchronized(this) {
            waitDownloadSet.size > 0 || onDownloadSet.size > 0
        }
    }

    private fun onSuccess(index: Int) {
        synchronized(this) {
            onDownloadSet.remove(index)
            successDownloadSet.add(index)
        }
    }

    private fun onErrorOrCancel(index: Int) {
        synchronized(this) {
            onDownloadSet.remove(index)
            waitDownloadSet.add(index)
        }
    }

    private fun onFinally() {
        synchronized(this) {
            if (waitDownloadSet.isEmpty() && onDownloadSet.isEmpty()) {
                postEvent(EventBus.UP_DOWNLOAD, "")
                cacheBookMap.remove(book.bookUrl)
            }
        }
    }

    /**
     * 从待下载列表内取第一条下载
     */
    fun download(scope: CoroutineScope, context: CoroutineContext): Boolean {
        synchronized(this) {
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
                addLog("${book.name}-${chapter.title} getContentSuccess")
                downloadFinish(chapter, content.ifBlank { "No content" })
            }.onError {
                //出现错误等待1秒后重新加入待下载列表
                delay(1000)
                onErrorOrCancel(chapterIndex)
                print(it.localizedMessage)
                addLog("${book.name}-${chapter.title} getContentError${it.localizedMessage}")
                downloadFinish(chapter, it.localizedMessage ?: "download error")
            }.onCancel {
                onErrorOrCancel(chapterIndex)
            }.onFinally {
                onFinally()
            }
            return true
        }
    }

    @Synchronized
    fun download(
        scope: CoroutineScope,
        chapter: BookChapter,
        resetPageOffset: Boolean = false
    ) {
        synchronized(this) {
            if (onDownloadSet.contains(chapter.index)) {
                return
            }
            onDownloadSet.add(chapter.index)
            WebBook.getContent(scope, bookSource, book, chapter)
                .onSuccess { content ->
                    onSuccess(chapter.index)
                    downloadFinish(chapter, content.ifBlank { "No content" }, resetPageOffset)
                }.onError {
                    onErrorOrCancel(chapter.index)
                    downloadFinish(
                        chapter,
                        it.localizedMessage ?: "download error",
                        resetPageOffset
                    )
                }.onCancel {
                    onErrorOrCancel(chapter.index)
                }.onFinally {
                    if (waitDownloadSet.isEmpty() && onDownloadSet.isEmpty()) {
                        postEvent(EventBus.UP_DOWNLOAD, "")
                    }
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