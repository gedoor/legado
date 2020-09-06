package io.legado.app.model

import android.annotation.SuppressLint
import io.legado.app.data.entities.*
import io.legado.app.help.coroutine.CompositeCoroutine
import io.legado.app.model.rss.Rss
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.htmlFormat
import io.legado.app.utils.isAbsUrl
import io.legado.app.utils.msg
import java.text.SimpleDateFormat
import java.util.*

object Debug {
    private var debugSource: String? = null
    var callback: Callback? = null
    private val tasks: CompositeCoroutine = CompositeCoroutine()

    @SuppressLint("ConstantLocale")
    private val DEBUG_TIME_FORMAT = SimpleDateFormat("[mm:ss.SSS]", Locale.getDefault())
    private var startTime: Long = System.currentTimeMillis()

    @Synchronized
    fun log(
        sourceUrl: String?,
        msg: String? = "",
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

    fun startDebug(rssSource: RssSource) {
        cancelDebug()
        debugSource = rssSource.sourceUrl
        log(debugSource, "︾开始解析")
        val sort = rssSource.sortUrls().entries.first()
        Rss.getArticles(sort.key, sort.value, rssSource, 1)
            .onSuccess {
                if (it.articles.isEmpty()) {
                    log(debugSource, "⇒列表页解析成功，为空")
                    log(debugSource, "︽解析完成", state = 1000)
                } else {
                    val ruleContent = rssSource.ruleContent
                    if (!rssSource.ruleArticles.isNullOrBlank() && rssSource.ruleDescription.isNullOrBlank()) {
                        log(debugSource, "︽列表页解析完成")
                        log(debugSource, showTime = false)
                        if (ruleContent.isNullOrEmpty()) {
                            log(debugSource, "⇒内容规则为空，默认获取整个网页", state = 1000)
                        } else {
                            rssContentDebug(it.articles[0], ruleContent, rssSource)
                        }
                    } else {
                        log(debugSource, "⇒存在描述规则，不解析内容页")
                        log(debugSource, "︽解析完成", state = 1000)
                    }
                }
            }
            .onError {
                log(debugSource, it.msg, state = -1)
            }
    }

    private fun rssContentDebug(rssArticle: RssArticle, ruleContent: String, rssSource: RssSource) {
        log(debugSource, "︾开始解析内容页")
        Rss.getContent(rssArticle, ruleContent, rssSource)
            .onSuccess {
                log(debugSource, it)
                log(debugSource, "︽内容页解析完成", state = 1000)
            }
            .onError {
                log(debugSource, it.msg, state = -1)
            }
    }

    fun startDebug(webBook: WebBook, key: String) {
        cancelDebug()
        debugSource = webBook.sourceUrl
        startTime = System.currentTimeMillis()
        when {
            key.isAbsUrl() -> {
                val book = Book()
                book.origin = webBook.sourceUrl
                book.bookUrl = key
                log(webBook.sourceUrl, "⇒开始访问详情页:$key")
                infoDebug(webBook, book)
            }
            key.contains("::") -> {
                val url = key.substring(key.indexOf("::") + 2)
                log(webBook.sourceUrl, "⇒开始访问发现页:$url")
                exploreDebug(webBook, url)
            }
            key.startsWith("++")-> {
                val url = key.substring(2)
                val book = Book()
                book.origin = webBook.sourceUrl
                book.tocUrl = url
                log(webBook.sourceUrl, "⇒开始访目录页:$url")
                tocDebug(webBook, book)
            }
            key.startsWith("--")-> {
                val url = key.substring(2)
                val book = Book()
                book.origin = webBook.sourceUrl
                log(webBook.sourceUrl, "⇒开始访正文页:$url")
                val chapter = BookChapter()
                chapter.title = "调试"
                chapter.url = url
                contentDebug(webBook, book, chapter, null)
            }
            else -> {
                log(webBook.sourceUrl, "⇒开始搜索关键字:$key")
                searchDebug(webBook, key)
            }
        }
    }

    private fun exploreDebug(webBook: WebBook, url: String) {
        log(debugSource, "︾开始解析发现页")
        val variableBook = SearchBook()
        val explore = webBook.exploreBook(url, 1, variableBook)
            .onSuccess { exploreBooks ->
                if (exploreBooks.isNotEmpty()) {
                    log(debugSource, "︽发现页解析完成")
                    log(debugSource, showTime = false)
                    infoDebug(webBook, exploreBooks[0].toBook())
                } else {
                    log(debugSource, "︽未获取到书籍", state = -1)
                }
            }
            .onError {
                log(debugSource, it.msg, state = -1)
            }
        tasks.add(explore)
    }

    private fun searchDebug(webBook: WebBook, key: String) {
        log(debugSource, "︾开始解析搜索页")
        val variableBook = SearchBook()
        val search = webBook.searchBook(key, 1, variableBook)
            .onSuccess { searchBooks ->
                if (searchBooks.isNotEmpty()) {
                    log(debugSource, "︽搜索页解析完成")
                    log(debugSource, showTime = false)
                    infoDebug(webBook, searchBooks[0].toBook())
                } else {
                    log(debugSource, "︽未获取到书籍", state = -1)
                }
            }
            .onError {
                log(debugSource, it.msg, state = -1)
            }
        tasks.add(search)
    }

    private fun infoDebug(webBook: WebBook, book: Book) {
        log(debugSource, "︾开始解析详情页")
        val info = webBook.getBookInfo(book)
            .onSuccess {
                log(debugSource, "︽详情页解析完成")
                log(debugSource, showTime = false)
                tocDebug(webBook, book)
            }
            .onError {
                log(debugSource, it.msg, state = -1)
            }
        tasks.add(info)
    }

    private fun tocDebug(webBook: WebBook, book: Book) {
        log(debugSource, "︾开始解析目录页")
        val chapterList = webBook.getChapterList(book)
            .onSuccess {
                if (it.isNotEmpty()) {
                    log(debugSource, "︽目录页解析完成")
                    log(debugSource, showTime = false)
                    val nextChapterUrl = if (it.size > 1) it[1].url else null
                    contentDebug(webBook, book, it[0], nextChapterUrl)
                } else {
                    log(debugSource, "︽目录列表为空", state = -1)
                }
            }
            .onError {
                log(debugSource, it.msg, state = -1)
            }
        tasks.add(chapterList)
    }

    private fun contentDebug(
        webBook: WebBook,
        book: Book,
        bookChapter: BookChapter,
        nextChapterUrl: String?
    ) {
        log(debugSource, "︾开始解析正文页")
        val content = webBook.getContent(book, bookChapter, nextChapterUrl)
            .onSuccess {
                log(debugSource, "︽正文页解析完成", state = 1000)
            }
            .onError {
                log(debugSource, it.msg, state = -1)
            }
        tasks.add(content)
    }

    interface Callback {
        fun printLog(state: Int, msg: String)
    }

}