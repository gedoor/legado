package io.legado.app.model

import android.annotation.SuppressLint
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssSource
import io.legado.app.help.coroutine.CompositeCoroutine
import io.legado.app.model.rss.Rss
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.HtmlFormatter
import io.legado.app.utils.isAbsUrl
import io.legado.app.utils.msg
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.*

object Debug {
    var callback: Callback? = null
    private var debugSource: String? = null
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
            printMsg = HtmlFormatter.format(msg)
        }
        if (showTime) {
            printMsg =
                "${DEBUG_TIME_FORMAT.format(Date(System.currentTimeMillis() - startTime))} $printMsg"
        }
        callback?.printLog(state, printMsg)
    }

    @Synchronized
    fun log(msg: String?) {
        log(debugSource, msg, true)
    }

    fun cancelDebug(destroy: Boolean = false) {
        tasks.clear()

        if (destroy) {
            debugSource = null
            callback = null
        }
    }

    fun startDebug(scope: CoroutineScope, rssSource: RssSource) {
        cancelDebug()
        debugSource = rssSource.sourceUrl
        log(debugSource, "︾开始解析")
        val sort = rssSource.sortUrls().entries.first()
        Rss.getArticles(scope, sort.key, sort.value, rssSource, 1)
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
                            rssContentDebug(scope, it.articles[0], ruleContent, rssSource)
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

    private fun rssContentDebug(
        scope: CoroutineScope,
        rssArticle: RssArticle,
        ruleContent: String,
        rssSource: RssSource
    ) {
        log(debugSource, "︾开始解析内容页")
        Rss.getContent(scope, rssArticle, ruleContent, rssSource)
            .onSuccess {
                log(debugSource, it)
                log(debugSource, "︽内容页解析完成", state = 1000)
            }
            .onError {
                log(debugSource, it.msg, state = -1)
            }
    }

    fun startDebug(scope: CoroutineScope, webBook: WebBook, key: String) {
        cancelDebug()
        debugSource = webBook.sourceUrl
        startTime = System.currentTimeMillis()
        when {
            key.isAbsUrl() -> {
                val book = Book()
                book.origin = webBook.sourceUrl
                book.bookUrl = key
                log(webBook.sourceUrl, "⇒开始访问详情页:$key")
                infoDebug(scope, webBook, book)
            }
            key.contains("::") -> {
                val url = key.substring(key.indexOf("::") + 2)
                log(webBook.sourceUrl, "⇒开始访问发现页:$url")
                exploreDebug(scope, webBook, url)
            }
            key.startsWith("++")-> {
                val url = key.substring(2)
                val book = Book()
                book.origin = webBook.sourceUrl
                book.tocUrl = url
                log(webBook.sourceUrl, "⇒开始访目录页:$url")
                tocDebug(scope, webBook, book)
            }
            key.startsWith("--")-> {
                val url = key.substring(2)
                val book = Book()
                book.origin = webBook.sourceUrl
                log(webBook.sourceUrl, "⇒开始访正文页:$url")
                val chapter = BookChapter()
                chapter.title = "调试"
                chapter.url = url
                contentDebug(scope, webBook, book, chapter, null)
            }
            else -> {
                log(webBook.sourceUrl, "⇒开始搜索关键字:$key")
                searchDebug(scope, webBook, key)
            }
        }
    }

    private fun exploreDebug(scope: CoroutineScope, webBook: WebBook, url: String) {
        log(debugSource, "︾开始解析发现页")
        val explore = webBook.exploreBook(scope, url, 1)
            .onSuccess { exploreBooks ->
                if (exploreBooks.isNotEmpty()) {
                    log(debugSource, "︽发现页解析完成")
                    log(debugSource, showTime = false)
                    infoDebug(scope, webBook, exploreBooks[0].toBook())
                } else {
                    log(debugSource, "︽未获取到书籍", state = -1)
                }
            }
            .onError {
                log(debugSource, it.msg, state = -1)
            }
        tasks.add(explore)
    }

    private fun searchDebug(scope: CoroutineScope, webBook: WebBook, key: String) {
        log(debugSource, "︾开始解析搜索页")
        val search = webBook.searchBook(scope, key, 1)
            .onSuccess { searchBooks ->
                if (searchBooks.isNotEmpty()) {
                    log(debugSource, "︽搜索页解析完成")
                    log(debugSource, showTime = false)
                    infoDebug(scope, webBook, searchBooks[0].toBook())
                } else {
                    log(debugSource, "︽未获取到书籍", state = -1)
                }
            }
            .onError {
                log(debugSource, it.msg, state = -1)
            }
        tasks.add(search)
    }

    private fun infoDebug(scope: CoroutineScope, webBook: WebBook, book: Book) {
        if (book.tocUrl.isNotBlank()) {
            log(debugSource, "目录url不为空,详情页已解析")
            log(debugSource, showTime = false)
            tocDebug(scope, webBook, book)
            return
        }
        log(debugSource, "︾开始解析详情页")
        val info = webBook.getBookInfo(scope, book)
            .onSuccess {
                log(debugSource, "︽详情页解析完成")
                log(debugSource, showTime = false)
                tocDebug(scope, webBook, book)
            }
            .onError {
                log(debugSource, it.msg, state = -1)
            }
        tasks.add(info)
    }

    private fun tocDebug(scope: CoroutineScope, webBook: WebBook, book: Book) {
        log(debugSource, "︾开始解析目录页")
        val chapterList = webBook.getChapterList(scope, book)
            .onSuccess {
                if (it.isNotEmpty()) {
                    log(debugSource, "︽目录页解析完成")
                    log(debugSource, showTime = false)
                    val nextChapterUrl = it.getOrNull(1)?.url
                    contentDebug(scope, webBook, book, it[0], nextChapterUrl)
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
        scope: CoroutineScope,
        webBook: WebBook,
        book: Book,
        bookChapter: BookChapter,
        nextChapterUrl: String?
    ) {
        log(debugSource, "︾开始解析正文页")
        val content = webBook.getContent(scope, book, bookChapter, nextChapterUrl)
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