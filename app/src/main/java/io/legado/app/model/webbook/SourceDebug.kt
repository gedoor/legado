package io.legado.app.model.webbook

import android.annotation.SuppressLint
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.help.coroutine.CompositeCoroutine
import io.legado.app.model.WebBook
import io.legado.app.utils.htmlFormat
import io.legado.app.utils.isAbsUrl
import java.text.SimpleDateFormat
import java.util.*

class SourceDebug(private val webBook: WebBook, callback: Callback) {

    companion object {
        private var debugSource: String? = null
        private var callback: Callback? = null
        private val tasks: CompositeCoroutine = CompositeCoroutine()

        @SuppressLint("ConstantLocale")
        private val DEBUG_TIME_FORMAT = SimpleDateFormat("[mm:ss.SSS]", Locale.getDefault())
        private val startTime: Long = System.currentTimeMillis()

        fun printLog(
            sourceUrl: String?,
            state: Int,
            msg: String,
            print: Boolean = true,
            isHtml: Boolean = false,
            showTime: Boolean = true
        ) {
            if (debugSource != sourceUrl || callback == null || !print) return
            var printMsg = msg
            if (isHtml) {
                printMsg = printMsg.htmlFormat()
            }
            if (showTime) {
                printMsg =
                    String.format(
                        "%s %s",
                        DEBUG_TIME_FORMAT.format(Date(System.currentTimeMillis() - startTime)),
                        printMsg
                    )
            }
            callback?.printLog(state, printMsg)
        }

        fun cancelDebug(destroy: Boolean = false) {
            tasks.clear()

            if (destroy) {
                debugSource = null
                callback = null
            }
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

    private fun searchDebug(key: String) {
        val search = webBook.searchBook(key, 1)
            .onSuccess { searchBooks ->
                searchBooks?.let {
                    if (searchBooks.isNotEmpty()) {
                        printLog(debugSource, 1, "", showTime = false)
                        infoDebug(BookHelp.toBook(searchBooks[0]))
                    }
                }
            }
            .onError {
                printLog(debugSource, -1, it.localizedMessage)
            }
        tasks.add(search)
    }

    private fun infoDebug(book: Book) {
        printLog(debugSource, 1, "开始获取详情页")
        val info = webBook.getBookInfo(book)
            .onSuccess {
                printLog(debugSource, 1, "", showTime = false)
                tocDebug(book)
            }
            .onError {
                printLog(debugSource, -1, it.localizedMessage)
            }
        tasks.add(info)
    }

    private fun tocDebug(book: Book) {
        printLog(debugSource, 1, "开始获取目录页")
        val chapterList = webBook.getChapterList(book)
            .onSuccess { chapterList ->
                chapterList?.let {
                    if (it.isNotEmpty()) {
                        printLog(debugSource, 1, "", showTime = false)
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
        printLog(debugSource, 1, "开始获取内容")
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

    fun printLog(
        sourceUrl: String?,
        state: Int,
        msg: String,
        print: Boolean = true,
        isHtml: Boolean = false
    ): SourceDebug {
        SourceDebug.printLog(sourceUrl, state, msg, print, isHtml)
        return this
    }
}