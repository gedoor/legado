package io.legado.app.service.help

import android.content.Context
import android.content.Intent
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.IntentAction
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.model.webBook.WebBook
import io.legado.app.service.CacheBookService
import io.legado.app.utils.msg
import kotlinx.coroutines.Dispatchers.IO
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

object CacheBook {
    val logs = arrayListOf<String>()
    private val downloadMap = ConcurrentHashMap<String, CopyOnWriteArraySet<Int>>()

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
        Intent(context, CacheBookService::class.java).let {
            it.action = IntentAction.start
            it.putExtra("bookUrl", bookUrl)
            it.putExtra("start", start)
            it.putExtra("end", end)
            context.startService(it)
        }
    }

    fun remove(context: Context, bookUrl: String) {
        Intent(context, CacheBookService::class.java).let {
            it.action = IntentAction.remove
            it.putExtra("bookUrl", bookUrl)
            context.startService(it)
        }
    }

    fun stop(context: Context) {
        Intent(context, CacheBookService::class.java).let {
            it.action = IntentAction.stop
            context.startService(it)
        }
    }

    fun download(
        webBook: WebBook,
        book: Book,
        chapter: BookChapter,
        resetPageOffset: Boolean = false
    ) {
        if (downloadMap[book.bookUrl]?.contains(chapter.index) == true) {
            return
        }
        if (downloadMap[book.bookUrl] == null) {
            downloadMap[book.bookUrl] = CopyOnWriteArraySet()
        }
        downloadMap[book.bookUrl]?.add(chapter.index)
        webBook.getContent(book, chapter)
            .onSuccess(IO) { content ->
                if (ReadBook.book?.bookUrl == book.bookUrl) {
                    if (content.isEmpty()) {
                        ReadBook.contentLoadFinish(
                            book,
                            chapter,
                            App.INSTANCE.getString(R.string.content_empty),
                            resetPageOffset = resetPageOffset
                        )
                    } else {
                        BookHelp.saveContent(book, chapter, content)
                        ReadBook.contentLoadFinish(
                            book,
                            chapter,
                            content,
                            resetPageOffset = resetPageOffset
                        )
                    }
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
                downloadMap[book.bookUrl]?.remove(chapter.index)
                ReadBook.removeLoading(chapter.index)
            }
    }

}