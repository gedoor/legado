package io.legado.app.model.webbook

import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.BookHelp
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
                R.string.error_get_web_content,
                baseUrl
            )
        )
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取成功:$baseUrl")
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
        if (ruleList.startsWith("+")) {
            ruleList = ruleList.substring(1)
        }
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, "解析书籍列表")
        collections = analyzeRule.getElements(ruleList)
        if (collections.isEmpty() && bookSource.bookUrlPattern.isNullOrEmpty()) {
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, "列表为空,按详情页解析")
            getInfoItem(analyzeRule, bookSource, baseUrl)?.let { searchBook ->
                searchBook.bookInfoHtml = body
                bookList.add(searchBook)
            }
        } else {
            val ruleName = analyzeRule.splitSourceRule(bookListRule.name ?: "")
            val ruleBookUrl = analyzeRule.splitSourceRule(bookListRule.bookUrl ?: "")
            val ruleAuthor = analyzeRule.splitSourceRule(bookListRule.author ?: "")
            val ruleCoverUrl = analyzeRule.splitSourceRule(bookListRule.coverUrl ?: "")
            val ruleIntro = analyzeRule.splitSourceRule(bookListRule.intro ?: "")
            val ruleKind = analyzeRule.splitSourceRule(bookListRule.kind ?: "")
            val ruleLastChapter = analyzeRule.splitSourceRule(bookListRule.lastChapter ?: "")
            val ruleWordCount = analyzeRule.splitSourceRule(bookListRule.wordCount ?: "")
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, "列表数为${collections.size}")
            for ((index, item) in collections.withIndex()) {
                getSearchItem(
                    item,
                    analyzeRule,
                    bookSource,
                    baseUrl,
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

    private fun getInfoItem(
        analyzeRule: AnalyzeRule,
        bookSource: BookSource,
        baseUrl: String
    ): SearchBook? {
        val searchBook = SearchBook()
        searchBook.bookUrl = baseUrl
        searchBook.origin = bookSource.bookSourceUrl
        searchBook.originName = bookSource.bookSourceName
        searchBook.originOrder = bookSource.customOrder
        analyzeRule.setBook(searchBook)
        with(bookSource.getBookInfoRule()) {
            init?.let {
                if (it.isNotEmpty()) {
                    SourceDebug.printLog(bookSource.bookSourceUrl, 1, "执行详情页初始化规则")
                    analyzeRule.setContent(analyzeRule.getElement(it))
                }
            }
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取书名")
            searchBook.name = analyzeRule.getString(name ?: "") ?: ""
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, searchBook.name)
            if (searchBook.name.isNotEmpty()) {
                SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取作者")
                searchBook.author = BookHelp.formatAuthor(analyzeRule.getString(author ?: ""))
                SourceDebug.printLog(bookSource.bookSourceUrl, 1, searchBook.author)
                SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取分类")
                searchBook.kind = analyzeRule.getString(kind ?: "")
                SourceDebug.printLog(bookSource.bookSourceUrl, 1, searchBook.kind ?: "")
                SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取简介")
                searchBook.intro = analyzeRule.getString(intro ?: "")
                SourceDebug.printLog(
                    bookSource.bookSourceUrl, 1, searchBook.intro
                        ?: "", isHtml = true
                )
                SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取字数")
                searchBook.wordCount = analyzeRule.getString(wordCount ?: "")
                SourceDebug.printLog(bookSource.bookSourceUrl, 1, searchBook.wordCount ?: "")
                SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取封面Url")
                searchBook.coverUrl = analyzeRule.getString(coverUrl ?: "", true)
                SourceDebug.printLog(bookSource.bookSourceUrl, 1, searchBook.coverUrl ?: "")
                SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取最新章节")
                searchBook.latestChapterTitle = analyzeRule.getString(lastChapter ?: "")
                SourceDebug.printLog(
                    bookSource.bookSourceUrl, 1, searchBook.latestChapterTitle
                        ?: ""
                )
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
        ruleBookUrl: List<AnalyzeRule.SourceRule>,
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
        searchBook.originOrder = bookSource.customOrder
        analyzeRule.setBook(searchBook)
        analyzeRule.setContent(item)
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取书名", printLog)
        searchBook.name = analyzeRule.getString(ruleName)
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, searchBook.name, printLog)
        if (searchBook.name.isNotEmpty()) {
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取书籍Url", printLog)
            searchBook.bookUrl = analyzeRule.getString(ruleBookUrl, true)
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, searchBook.bookUrl, printLog)
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取作者", printLog)
            searchBook.author = BookHelp.formatAuthor(analyzeRule.getString(ruleAuthor))
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, searchBook.author, printLog)
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取分类", printLog)
            searchBook.kind = analyzeRule.getString(ruleKind)
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, searchBook.kind ?: "", printLog)
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取简介", printLog)
            searchBook.intro = analyzeRule.getString(ruleIntro)
            SourceDebug.printLog(
                bookSource.bookSourceUrl, 1, searchBook.intro
                    ?: "", printLog, true
            )
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取字数", printLog)
            searchBook.wordCount = analyzeRule.getString(ruleWordCount)
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, searchBook.wordCount ?: "", printLog)
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取封面Url", printLog)
            searchBook.coverUrl = analyzeRule.getString(ruleCoverUrl, true)
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, searchBook.coverUrl ?: "", printLog)
            SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取最新章节", printLog)
            searchBook.latestChapterTitle = analyzeRule.getString(ruleLastChapter)
            SourceDebug.printLog(
                bookSource.bookSourceUrl, 1, searchBook.latestChapterTitle
                    ?: "", printLog
            )
            return searchBook
        }
        return null
    }
}