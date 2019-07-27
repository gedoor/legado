package io.legado.app.model.webbook

import android.annotation.SuppressLint
import io.legado.app.App
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookHelp
import io.legado.app.model.WebBook
import io.legado.app.utils.htmlFormat
import java.text.SimpleDateFormat
import java.util.*

class Debug(sourceUrl: String) {

    companion object {
        var debugSource: String? = null
        var callback: Callback? = null
        @SuppressLint("ConstantLocale")
        private val DEBUG_TIME_FORMAT = SimpleDateFormat("[mm:ss.SSS]", Locale.getDefault())
        private val startTime: Long = System.currentTimeMillis()

        fun printLog(source: String?, state: Int, msg: String, print: Boolean = true, isHtml: Boolean = false) {
            if (debugSource != source) return
            if (!print) return
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

    private var bookSource: BookSource? = null

    init {
        debugSource = sourceUrl
        bookSource = App.db.bookSourceDao().findByKey(sourceUrl)
    }

    fun searchDebug(key: String) {
        bookSource?.let {
            WebBook(it).searchBook(key, 1)
                .onSuccess { searchBooks ->
                    searchBooks?.let {
                        if (searchBooks.isNotEmpty()) {
                            infoDebug(BookHelp.toBook(searchBooks[0]))
                        }
                    }
                }

        } ?: printLog(debugSource, -1, "未找到书源")
    }

    fun infoDebug(book: Book) {
        bookSource?.let {
            WebBook(it).getBookInfo(book)
        } ?: printLog(debugSource, -1, "未找到书源")
    }


}