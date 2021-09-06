package io.legado.app.model

import android.annotation.SuppressLint
import android.content.Context
import io.legado.app.constant.IntentAction
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookHelp
import io.legado.app.model.webBook.WebBook
import io.legado.app.service.CacheBookService
import io.legado.app.utils.startService
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.coroutines.CoroutineContext

class CacheBook(val bookSource: BookSource, val book: Book) {

    companion object {

        val logs = arrayListOf<String>()
        val cacheBookMap = hashMapOf<String, CacheBook>()

        @SuppressLint("ConstantLocale")
        private val logTimeFormat = SimpleDateFormat("[mm:ss.SSS]", Locale.getDefault())

        @Synchronized
        fun get(bookUrl: String): CacheBook? {
            var cacheBook = cacheBookMap[bookUrl]
            if (cacheBook != null) {
                return cacheBook
            }
            val book = appDb.bookDao.getBook(bookUrl) ?: return null
            val bookSource = appDb.bookSourceDao.getBookSource(book.origin) ?: return null
            cacheBook = CacheBook(bookSource, book)
            cacheBookMap[bookUrl] = cacheBook
            return cacheBook
        }

        @Synchronized
        fun get(bookSource: BookSource, book: Book): CacheBook {
            var cacheBook = cacheBookMap[book.bookUrl]
            if (cacheBook != null) {
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

    val waitDownloadSet = CopyOnWriteArraySet<Int>()
    val successDownloadSet = CopyOnWriteArraySet<Int>()
    val onDownloadSet = CopyOnWriteArraySet<Int>()

    fun addDownload(start: Int, end: Int) {
        for (i in start..end) {
            waitDownloadSet.add(i)
        }
    }

    @Synchronized
    fun download(scope: CoroutineScope, context: CoroutineContext): Boolean {
        val chapterIndex = waitDownloadSet.firstOrNull() ?: return false
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
            waitConcurrent = true,
            context = context
        ).onSuccess { content ->
            onDownloadSet.remove(chapterIndex)
            successDownloadSet.add(chapterIndex)
            addLog("${book.name}-${chapter.title} getContentSuccess")
            downloadFinish(chapter, content.ifBlank { "No content" })
        }.onError {
            onDownloadSet.remove(chapterIndex)
            waitDownloadSet.add(chapterIndex)
            addLog("${book.name}-${chapter.title} getContentError${it.localizedMessage}")
            downloadFinish(chapter, it.localizedMessage ?: "download error")
        }.onCancel {
            onDownloadSet.remove(chapterIndex)
            waitDownloadSet.add(chapterIndex)
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
        WebBook.getContent(scope, bookSource, book, chapter)
            .onSuccess { content ->
                downloadFinish(chapter, content.ifBlank { "No content" }, resetPageOffset)
            }.onError {
                downloadFinish(chapter, it.localizedMessage ?: "download error", resetPageOffset)
            }.onFinally {
                onDownloadSet.remove(chapter.index)
                ReadBook.removeLoading(chapter.index)
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