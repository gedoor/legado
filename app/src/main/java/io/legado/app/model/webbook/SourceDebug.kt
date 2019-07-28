package io.legado.app.model.webbook

import android.annotation.SuppressLint
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.model.WebBook
import io.legado.app.utils.htmlFormat
import java.text.SimpleDateFormat
import java.util.*

class SourceDebug(private val webBook: WebBook, callback: Callback) {

    companion object {
        var debugSource: String? = null
        var callback: Callback? = null
        @SuppressLint("ConstantLocale")
        private val DEBUG_TIME_FORMAT = SimpleDateFormat("[mm:ss.SSS]", Locale.getDefault())
        private val startTime: Long = System.currentTimeMillis()

        fun printLog(source: String?, state: Int, msg: String, print: Boolean = true, isHtml: Boolean = false) {
            if (!print) return
            if (debugSource != source) return
            var printMsg = msg
            if (isHtml) {
                printMsg = printMsg.htmlFormat()
            }
            printMsg =
                String.format("%s %s", DEBUG_TIME_FORMAT.format(Date(System.currentTimeMillis() - startTime)), printMsg)
            callback?.printLog(state, printMsg)
        }
    }

    interface Callback {
        fun printLog(state: Int, msg: String)
    }

    init {
        SourceDebug.callback = callback
    }

    fun searchDebug(key: String) {
        webBook.searchBook(key, 1)
            .onSuccess { searchBooks ->
                searchBooks?.let {
                    if (searchBooks.isNotEmpty()) {
                        infoDebug(BookHelp.toBook(searchBooks[0]))
                    }
                }
            }
            .onError {
                printLog(debugSource, -1, it.localizedMessage)
            }
    }

    fun infoDebug(book: Book) {
        webBook.getBookInfo(book)
            .onSuccess {
                tocDebug(book)
            }
            .onError {
                printLog(debugSource, -1, it.localizedMessage)
            }
    }

    private fun tocDebug(book: Book) {
        webBook.getChapterList(book)
            .onSuccess { chapterList ->
                chapterList?.let {
                    if (it.isNotEmpty()) {
                        contentDebug(book, it[0])
                    }
                }
            }
            .onError {
                printLog(debugSource, -1, it.localizedMessage)
            }
    }

    private fun contentDebug(book: Book, bookChapter: BookChapter) {
        webBook.getContent(book, bookChapter)
            .onSuccess { content ->
                content?.let {
                    printLog(debugSource, 1000, it)
                }
            }
            .onError {
                printLog(debugSource, -1, it.localizedMessage)
            }
    }
}