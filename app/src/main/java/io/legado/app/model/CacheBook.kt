package io.legado.app.model

import android.content.Context
import io.legado.app.R
import io.legado.app.constant.IntentAction
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.model.webBook.WebBook
import io.legado.app.service.CacheBookService
import io.legado.app.utils.msg
import io.legado.app.utils.startService
import kotlinx.coroutines.CoroutineScope
import splitties.init.appCtx
import java.util.concurrent.CopyOnWriteArraySet

class CacheBook(val bookSource: BookSource, val book: Book) {

    companion object {

        val logs = arrayListOf<String>()
        private val cacheBookMap = hashMapOf<String, CacheBook>()

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
                logs.add(log)
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

        val downloadCount: Int
            get() {
                var count = 0
                cacheBookMap.forEach {
                    count += it.value.downloadSet.size
                }
                return count
            }

    }

    val downloadSet = CopyOnWriteArraySet<Int>()

    fun download(
        scope: CoroutineScope,
        chapter: BookChapter,
        resetPageOffset: Boolean = false
    ) {
        if (downloadSet.contains(chapter.index)) {
            return
        }
        downloadSet.add(chapter.index)
        WebBook.getContent(scope, bookSource, book, chapter)
            .onSuccess { content ->
                if (ReadBook.book?.bookUrl == book.bookUrl) {
                    ReadBook.contentLoadFinish(
                        book,
                        chapter,
                        content.ifBlank { appCtx.getString(R.string.content_empty) },
                        resetPageOffset = resetPageOffset
                    )
                }
            }.onError {
                if (ReadBook.book?.bookUrl == book.bookUrl) {
                    ReadBook.contentLoadFinish(
                        book,
                        chapter,
                        it.msg,
                        resetPageOffset = resetPageOffset
                    )
                }
            }.onFinally {
                downloadSet.remove(chapter.index)
                ReadBook.removeLoading(chapter.index)
            }
    }

}