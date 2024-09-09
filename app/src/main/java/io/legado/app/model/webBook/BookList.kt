package io.legado.app.model.webBook

import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.rule.BookListRule
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.getBookType
import io.legado.app.model.Debug
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.analyzeRule.RuleData
import io.legado.app.utils.HtmlFormatter
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.StringUtils.wordCountFormat
import kotlinx.coroutines.ensureActive
import splitties.init.appCtx
import kotlin.coroutines.coroutineContext

/**
 * 获取书籍列表
 */
object BookList {

    @Throws(Exception::class)
    suspend fun analyzeBookList(
        bookSource: BookSource,
        ruleData: RuleData,
        analyzeUrl: AnalyzeUrl,
        baseUrl: String,
        body: String?,
        isSearch: Boolean = true,
    ): ArrayList<SearchBook> {
        body ?: throw NoStackTraceException(
            appCtx.getString(
                R.string.error_get_web_content,
                analyzeUrl.ruleUrl
            )
        )
        val bookList = ArrayList<SearchBook>()
        Debug.log(bookSource.bookSourceUrl, "≡获取成功:${analyzeUrl.ruleUrl}")
        Debug.log(bookSource.bookSourceUrl, body, state = 10)
        val analyzeRule = AnalyzeRule(ruleData, bookSource)
        analyzeRule.setContent(body).setBaseUrl(baseUrl)
        analyzeRule.setRedirectUrl(baseUrl)
        analyzeRule.setCoroutineContext(coroutineContext)
        if (isSearch) bookSource.bookUrlPattern?.let {
            coroutineContext.ensureActive()
            if (baseUrl.matches(it.toRegex())) {
                Debug.log(bookSource.bookSourceUrl, "≡链接为详情页")
                getInfoItem(
                    bookSource,
                    analyzeRule,
                    analyzeUrl,
                    body,
                    baseUrl,
                    ruleData.getVariable()
                )?.let { searchBook ->
                    searchBook.infoHtml = body
                    bookList.add(searchBook)
                }
                return bookList
            }
        }
        val collections: List<Any>
        var reverse = false
        val bookListRule: BookListRule = when {
            isSearch -> bookSource.getSearchRule()
            bookSource.getExploreRule().bookList.isNullOrBlank() -> bookSource.getSearchRule()
            else -> bookSource.getExploreRule()
        }
        var ruleList: String = bookListRule.bookList ?: ""
        if (ruleList.startsWith("-")) {
            reverse = true
            ruleList = ruleList.substring(1)
        }
        if (ruleList.startsWith("+")) {
            ruleList = ruleList.substring(1)
        }
        Debug.log(bookSource.bookSourceUrl, "┌获取书籍列表")
        collections = analyzeRule.getElements(ruleList)
        coroutineContext.ensureActive()
        if (collections.isEmpty() && bookSource.bookUrlPattern.isNullOrEmpty()) {
            Debug.log(bookSource.bookSourceUrl, "└列表为空,按详情页解析")
            getInfoItem(
                bookSource, analyzeRule, analyzeUrl, body, baseUrl, ruleData.getVariable()
            )?.let { searchBook ->
                searchBook.infoHtml = body
                bookList.add(searchBook)
            }
        } else {
            val ruleName = analyzeRule.splitSourceRule(bookListRule.name)
            val ruleBookUrl = analyzeRule.splitSourceRule(bookListRule.bookUrl)
            val ruleAuthor = analyzeRule.splitSourceRule(bookListRule.author)
            val ruleCoverUrl = analyzeRule.splitSourceRule(bookListRule.coverUrl)
            val ruleIntro = analyzeRule.splitSourceRule(bookListRule.intro)
            val ruleKind = analyzeRule.splitSourceRule(bookListRule.kind)
            val ruleLastChapter = analyzeRule.splitSourceRule(bookListRule.lastChapter)
            val ruleWordCount = analyzeRule.splitSourceRule(bookListRule.wordCount)
            Debug.log(bookSource.bookSourceUrl, "└列表大小:${collections.size}")
            for ((index, item) in collections.withIndex()) {
                getSearchItem(
                    bookSource, analyzeRule, item, baseUrl, ruleData.getVariable(),
                    index == 0,
                    ruleName = ruleName,
                    ruleBookUrl = ruleBookUrl,
                    ruleAuthor = ruleAuthor,
                    ruleCoverUrl = ruleCoverUrl,
                    ruleIntro = ruleIntro,
                    ruleKind = ruleKind,
                    ruleLastChapter = ruleLastChapter,
                    ruleWordCount = ruleWordCount
                )?.let { searchBook ->
                    if (baseUrl == searchBook.bookUrl) {
                        searchBook.infoHtml = body
                    }
                    bookList.add(searchBook)
                }
            }
            val lh = LinkedHashSet(bookList)
            bookList.clear()
            bookList.addAll(lh)
            if (reverse) {
                bookList.reverse()
            }
        }
        Debug.log(bookSource.bookSourceUrl, "◇书籍总数:${bookList.size}")
        return bookList
    }

    @Throws(Exception::class)
    private suspend fun getInfoItem(
        bookSource: BookSource,
        analyzeRule: AnalyzeRule,
        analyzeUrl: AnalyzeUrl,
        body: String,
        baseUrl: String,
        variable: String?
    ): SearchBook? {
        val book = Book(variable = variable)
        book.bookUrl = NetworkUtils.getAbsoluteURL(analyzeUrl.url, analyzeUrl.ruleUrl)
        book.origin = bookSource.bookSourceUrl
        book.originName = bookSource.bookSourceName
        book.originOrder = bookSource.customOrder
        book.type = bookSource.getBookType()
        analyzeRule.ruleData = book
        BookInfo.analyzeBookInfo(
            book,
            body,
            analyzeRule,
            bookSource,
            baseUrl,
            baseUrl,
            false
        )
        if (book.name.isNotBlank()) {
            return book.toSearchBook()
        }
        return null
    }

    @Throws(Exception::class)
    private suspend fun getSearchItem(
        bookSource: BookSource,
        analyzeRule: AnalyzeRule,
        item: Any,
        baseUrl: String,
        variable: String?,
        log: Boolean,
        ruleName: List<AnalyzeRule.SourceRule>,
        ruleBookUrl: List<AnalyzeRule.SourceRule>,
        ruleAuthor: List<AnalyzeRule.SourceRule>,
        ruleKind: List<AnalyzeRule.SourceRule>,
        ruleCoverUrl: List<AnalyzeRule.SourceRule>,
        ruleWordCount: List<AnalyzeRule.SourceRule>,
        ruleIntro: List<AnalyzeRule.SourceRule>,
        ruleLastChapter: List<AnalyzeRule.SourceRule>
    ): SearchBook? {
        val searchBook = SearchBook(variable = variable)
        searchBook.type = bookSource.getBookType()
        searchBook.origin = bookSource.bookSourceUrl
        searchBook.originName = bookSource.bookSourceName
        searchBook.originOrder = bookSource.customOrder
        analyzeRule.ruleData = searchBook
        analyzeRule.setContent(item)
        coroutineContext.ensureActive()
        Debug.log(bookSource.bookSourceUrl, "┌获取书名", log)
        searchBook.name = BookHelp.formatBookName(analyzeRule.getString(ruleName))
        Debug.log(bookSource.bookSourceUrl, "└${searchBook.name}", log)
        if (searchBook.name.isNotEmpty()) {
            coroutineContext.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌获取作者", log)
            searchBook.author = BookHelp.formatBookAuthor(analyzeRule.getString(ruleAuthor))
            Debug.log(bookSource.bookSourceUrl, "└${searchBook.author}", log)
            coroutineContext.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌获取分类", log)
            try {
                searchBook.kind = analyzeRule.getStringList(ruleKind)?.joinToString(",")
                Debug.log(bookSource.bookSourceUrl, "└${searchBook.kind ?: ""}", log)
            } catch (e: Exception) {
                Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}", log)
            }
            coroutineContext.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌获取字数", log)
            try {
                searchBook.wordCount = wordCountFormat(analyzeRule.getString(ruleWordCount))
                Debug.log(bookSource.bookSourceUrl, "└${searchBook.wordCount}", log)
            } catch (e: java.lang.Exception) {
                Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}", log)
            }
            coroutineContext.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌获取最新章节", log)
            try {
                searchBook.latestChapterTitle = analyzeRule.getString(ruleLastChapter)
                Debug.log(bookSource.bookSourceUrl, "└${searchBook.latestChapterTitle}", log)
            } catch (e: java.lang.Exception) {
                Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}", log)
            }
            coroutineContext.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌获取简介", log)
            try {
                searchBook.intro = HtmlFormatter.format(analyzeRule.getString(ruleIntro))
                Debug.log(bookSource.bookSourceUrl, "└${searchBook.intro}", log)
            } catch (e: java.lang.Exception) {
                Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}", log)
            }
            coroutineContext.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌获取封面链接", log)
            try {
                analyzeRule.getString(ruleCoverUrl).let {
                    if (it.isNotEmpty()) {
                        searchBook.coverUrl = NetworkUtils.getAbsoluteURL(baseUrl, it)
                    }
                }
                Debug.log(bookSource.bookSourceUrl, "└${searchBook.coverUrl ?: ""}", log)
            } catch (e: java.lang.Exception) {
                Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}", log)
            }
            coroutineContext.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌获取详情页链接", log)
            searchBook.bookUrl = analyzeRule.getString(ruleBookUrl, isUrl = true)
            if (searchBook.bookUrl.isEmpty()) {
                searchBook.bookUrl = baseUrl
            }
            Debug.log(bookSource.bookSourceUrl, "└${searchBook.bookUrl}", log)
            return searchBook
        }
        return null
    }

}