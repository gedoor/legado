package io.legado.app.model.webbook

import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.NetworkUtils
import retrofit2.Response

object BookList {

    @Throws(Exception::class)
    fun analyzeBookList(
        response: Response<String>,
        bookSource: BookSource,
        analyzeUrl: AnalyzeUrl,
        isSearch: Boolean = true
    ): ArrayList<SearchBook> {
        val bookList = ArrayList<SearchBook>()
        val baseUrl: String = NetworkUtils.getUrl(response)
        val body: String? = response.body()
        body ?: throw Exception(
            App.INSTANCE.getString(
                R.string.get_web_content_error,
                baseUrl
            )
        )
        val analyzeRule = AnalyzeRule(null)
        analyzeRule.setContent(body, baseUrl)
        bookSource.bookUrlPattern?.let {
            if (baseUrl.matches(it.toRegex())) {
                SourceDebug.printLog(bookSource.bookSourceUrl, 1, "url为详情页")
                getInfoItem(analyzeRule, bookSource, baseUrl)?.let { searchBook ->
                    searchBook.bookInfoHtml = body
                    bookList.add(searchBook)
                }
                return bookList
            }
        }
        val collections: List<Any>
        var reverse = false
        val bookListRule = if (isSearch) bookSource.getSearchRule() else bookSource.getExploreRule()
        var ruleList = bookListRule.bookList ?: ""
        if (ruleList.startsWith("-")) {
            reverse = true
            ruleList = ruleList.substring(1)
        }
        collections = analyzeRule.getElements(ruleList)
        if (collections.isEmpty() && bookSource.bookUrlPattern.isNullOrEmpty()) {
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, "列表为空,按详情页解析")
            getInfoItem(analyzeRule, bookSource, baseUrl)?.let { searchBook ->
                searchBook.bookInfoHtml = body
                bookList.add(searchBook)
            }
        } else {
            val ruleName = analyzeRule.splitSourceRule(bookListRule.name ?: "")
            val ruleAuthor = analyzeRule.splitSourceRule(bookListRule.author ?: "")
            val ruleCoverUrl = analyzeRule.splitSourceRule(bookListRule.coverUrl ?: "")
            val ruleIntro = analyzeRule.splitSourceRule(bookListRule.intro ?: "")
            val ruleKind = analyzeRule.splitSourceRule(bookListRule.kind ?: "")
            val ruleLastChapter = analyzeRule.splitSourceRule(bookListRule.lastChapter ?: "")
            val ruleWordCount = analyzeRule.splitSourceRule(bookListRule.wordCount ?: "")
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, "列表书为${collections.size}")
            for ((index, item) in collections.withIndex()) {
                getSearchItem(
                    item,
                    analyzeRule,
                    bookSource,
                    baseUrl,
                    index == 0,
                    ruleName = ruleName,
                    ruleAuthor = ruleAuthor,
                    ruleCoverUrl = ruleCoverUrl,
                    ruleIntro = ruleIntro,
                    ruleKind = ruleKind,
                    ruleLastChapter = ruleLastChapter,
                    ruleWordCount = ruleWordCount
                )?.let { searchBook ->
                    if (baseUrl == searchBook.bookUrl) {
                        searchBook.bookInfoHtml = body
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

    private fun getInfoItem(analyzeRule: AnalyzeRule, bookSource: BookSource, baseUrl: String): SearchBook? {
        val searchBook = SearchBook()
        searchBook.bookUrl = baseUrl
        searchBook.origin = bookSource.bookSourceUrl
        searchBook.originName = bookSource.bookSourceName
        analyzeRule.setBook(searchBook)
        with(bookSource.getBookInfoRule()) {
            init?.let {
                SourceDebug.printLog(bookSource.bookSourceUrl, 1, "执行详情页初始化规则")
                analyzeRule.setContent(analyzeRule.getElement(it))
            }
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取书名")
            searchBook.name = analyzeRule.getString(name ?: "")
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, searchBook.name ?: "")
            if (!searchBook.name.isNullOrEmpty()) {
                searchBook.author = analyzeRule.getString(author ?: "")
                searchBook.coverUrl = analyzeRule.getString(coverUrl ?: "")
                searchBook.intro = analyzeRule.getString(intro ?: "")
                searchBook.latestChapterTitle = analyzeRule.getString(lastChapter ?: "")
                searchBook.kind = analyzeRule.getString(kind ?: "")
                searchBook.wordCount = analyzeRule.getString(wordCount ?: "")

                return searchBook
            }
        }
        return null
    }

    private fun getSearchItem(
        item: Any,
        analyzeRule: AnalyzeRule,
        bookSource: BookSource,
        baseUrl: String,
        printLog: Boolean,
        ruleName: List<AnalyzeRule.SourceRule>,
        ruleAuthor: List<AnalyzeRule.SourceRule>,
        ruleKind: List<AnalyzeRule.SourceRule>,
        ruleCoverUrl: List<AnalyzeRule.SourceRule>,
        ruleWordCount: List<AnalyzeRule.SourceRule>,
        ruleIntro: List<AnalyzeRule.SourceRule>,
        ruleLastChapter: List<AnalyzeRule.SourceRule>
    ): SearchBook? {
        val searchBook = SearchBook()
        searchBook.origin = bookSource.bookSourceUrl
        searchBook.originName = bookSource.bookSourceName
        analyzeRule.setBook(searchBook)
        analyzeRule.setContent(item)
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取书名", printLog)
        searchBook.name = analyzeRule.getString(ruleName)
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, searchBook.name ?: "", printLog)
        searchBook.name?.let {
            searchBook.author = analyzeRule.getString(ruleAuthor)
            searchBook.kind = analyzeRule.getString(ruleKind)
            searchBook.intro = analyzeRule.getString(ruleIntro)
            searchBook.wordCount = analyzeRule.getString(ruleWordCount)
            searchBook.coverUrl = analyzeRule.getString(ruleCoverUrl)
            searchBook.latestChapterTitle = analyzeRule.getString(ruleLastChapter)
            return searchBook
        }
        return null
    }
}