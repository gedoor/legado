package io.legado.app.model.webbook

import android.annotation.SuppressLint
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.WebBook
import io.legado.app.utils.htmlFormat
import io.legado.app.utils.isAbsUrl
import java.text.SimpleDateFormat
import java.util.*

class SourceDebug(private val webBook: WebBook, callback: Callback) {

    companion object {
        private var debugSource: String? = null
        private var callback: Callback? = null
        private val tasks: MutableList<Coroutine<*>> = mutableListOf()

        @SuppressLint("ConstantLocale")
        private val DEBUG_TIME_FORMAT = SimpleDateFormat("[mm:ss.SSS]", Locale.getDefault())
        private val startTime: Long = System.currentTimeMillis()

        fun printLog(sourceUrl: String?, state: Int, msg: String, print: Boolean = true, isHtml: Boolean = false) {
            if (debugSource != sourceUrl) return
            if (!print) return
            var printMsg = msg
            if (isHtml) {
                printMsg = printMsg.htmlFormat()
            }
            printMsg =
                String.format("%s %s", DEBUG_TIME_FORMAT.format(Date(System.currentTimeMillis() - startTime)), printMsg)
            callback?.printLog(state, printMsg)
        }

        fun cancelDebug() {
            tasks.forEach {
                if (!it.isCancelled) {
                    it.cancel()
                }
            }
            tasks.clear()
        }

        fun stopDebug(){
            cancelDebug()
            debugSource = null
            callback = null
        }
    }

    init {
        debugSource = webBook.sourceUrl
        SourceDebug.callback = callback
    }

    fun startDebug(key: String) {
        cancelDebug()
        with(webBook) {
            if (key.isAbsUrl()) {
                val book = Book()
                book.origin = sourceUrl
                book.bookUrl = key
                printLog(sourceUrl, 1, "开始访问$key")
                infoDebug(book)
            } else {
                printLog(sourceUrl, 1, "开始搜索关键字$key")
                searchDebug(key)
            }
        }
    }

    fun searchDebug(key: String) {
        val search = webBook.searchBook(key, 1)
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
        tasks.add(search)
    }

    fun infoDebug(book: Book) {
        val info = webBook.getBookInfo(book)
            .onSuccess {
                tocDebug(book)
            }
            .onError {
                printLog(debugSource, -1, it.localizedMessage)
            }
        tasks.add(info)
    }

    private fun tocDebug(book: Book) {
        val chapterList = webBook.getChapterList(book)
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
        tasks.add(chapterList)
    }

    private fun contentDebug(book: Book, bookChapter: BookChapter) {
        val content = webBook.getContent(book, bookChapter)
            .onSuccess { content ->
                content?.let {
                    printLog(debugSource, 1000, it)
                }
            }
            .onError {
                printLog(debugSource, -1, it.localizedMessage)
            }
        tasks.add(content)
    }

    interface Callback {
        fun printLog(state: Int, msg: String)
    }
}