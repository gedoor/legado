package io.legado.app.model.webBook

import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.rule.BookListRule
import io.legado.app.help.BookHelp
import io.legado.app.model.Debug
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.StringUtils.wordCountFormat
import io.legado.app.utils.htmlFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

object BookList {

    @Throws(Exception::class)
    fun analyzeBookList(
        scope: CoroutineScope,
        body: String?,
        bookSource: BookSource,
        analyzeUrl: AnalyzeUrl,
        baseUrl: String,
        variableBook: SearchBook,
        isSearch: Boolean = true,
    ): ArrayList<SearchBook> {
        val bookList = ArrayList<SearchBook>()
        body ?: throw Exception(
            App.INSTANCE.getString(
                R.string.error_get_web_content,
                analyzeUrl.ruleUrl
            )
        )
        Debug.log(bookSource.bookSourceUrl, "≡获取成功:${analyzeUrl.ruleUrl}")
        val analyzeRule = AnalyzeRule(variableBook)
        analyzeRule.setContent(body).setBaseUrl(baseUrl)
        bookSource.bookUrlPattern?.let {
            scope.ensureActive()
            if (baseUrl.matches(it.toRegex())) {
                Debug.log(bookSource.bookSourceUrl, "≡链接为详情页")
                getInfoItem(scope, analyzeRule, bookSource, baseUrl, variableBook.variable)
                    ?.let { searchBook ->
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
        scope.ensureActive()
        if (collections.isEmpty() && bookSource.bookUrlPattern.isNullOrEmpty()) {
            Debug.log(bookSource.bookSourceUrl, "└列表为空,按详情页解析")
            getInfoItem(scope, analyzeRule, bookSource, baseUrl, variableBook.variable)
                ?.let { searchBook ->
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
                    scope,
                    item,
                    analyzeRule,
                    bookSource,
                    baseUrl,
                    variableBook.variable,
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
            if (reverse) {
                bookList.reverse()
            }
        }
        return bookList
    }

    @Throws(Exception::class)
    private fun getInfoItem(
        scope: CoroutineScope,
        analyzeRule: AnalyzeRule,
        bookSource: BookSource,
        baseUrl: String,
        variable: String?
    ): SearchBook? {
        val searchBook = SearchBook(variable = variable)
        searchBook.bookUrl = baseUrl
        searchBook.origin = bookSource.bookSourceUrl
        searchBook.originName = bookSource.bookSourceName
        searchBook.originOrder = bookSource.customOrder
        searchBook.type = bookSource.bookSourceType
        analyzeRule.book = searchBook
        with(bookSource.getBookInfoRule()) {
            init?.let {
                if (it.isNotEmpty()) {
                    scope.ensureActive()
                    Debug.log(bookSource.bookSourceUrl, "≡执行详情页初始化规则")
                    analyzeRule.setContent(analyzeRule.getElement(it))
                }
            }
            scope.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌获取书名")
            searchBook.name = BookHelp.formatBookName(analyzeRule.getString(name))
            Debug.log(bookSource.bookSourceUrl, "└${searchBook.name}")
            if (searchBook.name.isNotEmpty()) {
                scope.ensureActive()
                Debug.log(bookSource.bookSourceUrl, "┌获取作者")
                searchBook.author = BookHelp.formatBookAuthor(analyzeRule.getString(author))
                Debug.log(bookSource.bookSourceUrl, "└${searchBook.author}")
                scope.ensureActive()
                Debug.log(bookSource.bookSourceUrl, "┌获取分类")
                searchBook.kind = analyzeRule.getStringList(kind)?.joinToString(",")
                Debug.log(bookSource.bookSourceUrl, "└${searchBook.kind}")
                scope.ensureActive()
                Debug.log(bookSource.bookSourceUrl, "┌获取字数")
                searchBook.wordCount = wordCountFormat(analyzeRule.getString(wordCount))
                Debug.log(bookSource.bookSourceUrl, "└${searchBook.wordCount}")
                scope.ensureActive()
                Debug.log(bookSource.bookSourceUrl, "┌获取最新章节")
                searchBook.latestChapterTitle = analyzeRule.getString(lastChapter)
                Debug.log(bookSource.bookSourceUrl, "└${searchBook.latestChapterTitle}")
                scope.ensureActive()
                Debug.log(bookSource.bookSourceUrl, "┌获取简介")
                searchBook.intro = analyzeRule.getString(intro).htmlFormat()
                Debug.log(bookSource.bookSourceUrl, "└${searchBook.intro}")
                scope.ensureActive()
                Debug.log(bookSource.bookSourceUrl, "┌获取封面链接")
                searchBook.coverUrl = analyzeRule.getString(coverUrl, true)
                Debug.log(bookSource.bookSourceUrl, "└${searchBook.coverUrl}")
                return searchBook
            }
        }
        return null
    }

    @Throws(Exception::class)
    private fun getSearchItem(
        scope: CoroutineScope,
        item: Any,
        analyzeRule: AnalyzeRule,
        bookSource: BookSource,
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
        searchBook.origin = bookSource.bookSourceUrl
        searchBook.originName = bookSource.bookSourceName
        searchBook.type = bookSource.bookSourceType
        searchBook.originOrder = bookSource.customOrder
        analyzeRule.book = searchBook
        analyzeRule.setContent(item)
        scope.ensureActive()
        Debug.log(bookSource.bookSourceUrl, "┌获取书名", log)
        searchBook.name = BookHelp.formatBookName(analyzeRule.getString(ruleName))
        Debug.log(bookSource.bookSourceUrl, "└${searchBook.name}", log)
        if (searchBook.name.isNotEmpty()) {
            scope.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌获取作者", log)
            searchBook.author = BookHelp.formatBookAuthor(analyzeRule.getString(ruleAuthor))
            Debug.log(bookSource.bookSourceUrl, "└${searchBook.author}", log)
            scope.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌获取分类", log)
            searchBook.kind = analyzeRule.getStringList(ruleKind)?.joinToString(",")
            Debug.log(bookSource.bookSourceUrl, "└${searchBook.kind}", log)
            scope.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌获取字数", log)
            searchBook.wordCount = wordCountFormat(analyzeRule.getString(ruleWordCount))
            Debug.log(bookSource.bookSourceUrl, "└${searchBook.wordCount}", log)
            scope.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌获取最新章节", log)
            searchBook.latestChapterTitle = analyzeRule.getString(ruleLastChapter)
            Debug.log(bookSource.bookSourceUrl, "└${searchBook.latestChapterTitle}", log)
            scope.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌获取简介", log)
            searchBook.intro = analyzeRule.getString(ruleIntro).htmlFormat()
            Debug.log(bookSource.bookSourceUrl, "└${searchBook.intro}", log)
            scope.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌获取封面链接", log)
            analyzeRule.getString(ruleCoverUrl).let {
                if (it.isNotEmpty()) searchBook.coverUrl = NetworkUtils.getAbsoluteURL(baseUrl, it)
            }
            Debug.log(bookSource.bookSourceUrl, "└${searchBook.coverUrl}", log)
            scope.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌获取详情页链接", log)
            searchBook.bookUrl = analyzeRule.getString(ruleBookUrl, true)
            if (searchBook.bookUrl.isEmpty()) {
                searchBook.bookUrl = baseUrl
            }
            Debug.log(bookSource.bookSourceUrl, "└${searchBook.bookUrl}", log)
            return searchBook
        }
        return null
    }

}