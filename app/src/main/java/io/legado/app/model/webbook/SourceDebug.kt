package io.legado.app.model.webbook

import android.annotation.SuppressLint
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
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
        private var startTime: Long = System.currentTimeMillis()

        @Synchronized
        fun printLog(
            sourceUrl: String?,
            msg: String?,
            print: Boolean = true,
            isHtml: Boolean = false,
            showTime: Boolean = true,
            state: Int = 1
        ) {
            if (debugSource != sourceUrl || callback == null || !print) return
            var printMsg = msg ?: ""
            if (isHtml) {
                printMsg = printMsg.htmlFormat()
            }
            if (showTime) {
                printMsg =
                    "${DEBUG_TIME_FORMAT.format(Date(System.currentTimeMillis() - startTime))} $printMsg"
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
        startTime = System.currentTimeMillis()
        if (key.isAbsUrl()) {
            val book = Book()
            book.origin = webBook.sourceUrl
            book.bookUrl = key
            printLog(webBook.sourceUrl, "⇒开始访问详情页:$key")
            infoDebug(book)
        } else if (key.contains("::")) {
            val url = key.substring(key.indexOf("::") + 2)
            printLog(webBook.sourceUrl, "⇒开始访问发现页:$url")
            exploreDebug(url)
        } else {
            printLog(webBook.sourceUrl, "⇒开始搜索关键字:$key")
            searchDebug(key)
        }
    }

    private fun exploreDebug(url: String) {
        printLog(debugSource, "︾开始解析发现页")
        val explore = webBook.exploreBook(url, 1)
            .onSuccess { exploreBooks ->
                exploreBooks?.let {
                    if (exploreBooks.isNotEmpty()) {
                        printLog(debugSource, "︽发现页解析完成")
                        printLog(debugSource, "", showTime = false)
                        infoDebug(exploreBooks[0].toBook())
                    } else {
                        printLog(debugSource, "︽未获取到书籍", state = -1)
                    }
                }
            }
            .onError {
                printLog(debugSource, it.localizedMessage, state = -1)
            }
        tasks.add(explore)
    }

    private fun searchDebug(key: String) {
        printLog(debugSource, "︾开始解析搜索页")
        val search = webBook.searchBook(key, 1)
            .onSuccess { searchBooks ->
                searchBooks?.let {
                    if (searchBooks.isNotEmpty()) {
                        printLog(debugSource, "︽搜索页解析完成")
                        printLog(debugSource, "", showTime = false)
                        infoDebug(searchBooks[0].toBook())
                    } else {
                        printLog(debugSource, "︽未获取到书籍", state = -1)
                    }
                }
            }
            .onError {
                printLog(debugSource, it.localizedMessage, state = -1)
            }
        tasks.add(search)
    }

    private fun infoDebug(book: Book) {
        printLog(debugSource, "︾开始解析详情页")
        val info = webBook.getBookInfo(book)
            .onSuccess {
                printLog(debugSource, "︽详情页解析完成")
                printLog(debugSource, "", showTime = false)
                tocDebug(book)
            }
            .onError {
                printLog(debugSource, it.localizedMessage, state = -1)
            }
        tasks.add(info)
    }

    private fun tocDebug(book: Book) {
        printLog(debugSource, "︾开始解析目录页")
        val chapterList = webBook.getChapterList(book)
            .onSuccess { chapterList ->
                chapterList?.let {
                    if (it.isNotEmpty()) {
                        printLog(debugSource, "︽目录页解析完成")
                        printLog(debugSource, "", showTime = false)
                        val nextChapterUrl = if (it.size > 1) it[1].url else null
                        contentDebug(book, it[0], nextChapterUrl)
                    } else {
                        printLog(debugSource, "︽目录列表为空", state = -1)
                    }
                }
            }
            .onError {
                printLog(debugSource, it.localizedMessage, state = -1)
            }
        tasks.add(chapterList)
    }

    private fun contentDebug(book: Book, bookChapter: BookChapter, nextChapterUrl: String?) {
        printLog(debugSource, "︾开始解析正文页")
        val content = webBook.getContent(book, bookChapter, nextChapterUrl)
            .onSuccess { content ->
                content?.let {
                    printLog(debugSource, it, state = 1000)
                }
                printLog(debugSource, "︽正文页解析完成")
            }
            .onError {
                printLog(debugSource, it.localizedMessage, state = -1)
            }
        tasks.add(content)
    }

    interface Callback {
        fun printLog(state: Int, msg: String)
    }

}