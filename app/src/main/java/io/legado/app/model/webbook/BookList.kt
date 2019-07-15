package io.legado.app.model.webbook

import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.rule.BookListRule
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.NetworkUtils
import org.mozilla.javascript.NativeObject
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
        val analyzer = AnalyzeRule(null)
        analyzer.setContent(body, baseUrl)
        bookSource.bookUrlPattern?.let {
            if (baseUrl.matches(it.toRegex())) {
                getInfoItem(analyzer, bookSource, baseUrl)?.let { searchBook ->
                    searchBook.bookInfoHtml = body
                    bookList.add(searchBook)
                }
                return bookList
            }
        }
        val collections: List<Any>
        var reverse = false
        var allInOne = false
        val bookListRule = if (isSearch) bookSource.getSearchRule() else bookSource.getExploreRule()
        var ruleList = bookListRule.bookList ?: ""
        if (ruleList.startsWith("-")) {
            reverse = true
            ruleList = ruleList.substring(1)
        }
        if (ruleList.startsWith(":")) {
            ruleList = ruleList.substring(1)
        }
        if (ruleList.startsWith("+")) {
            allInOne = true
            ruleList = ruleList.substring(1)
        }
        collections = analyzer.getElements(ruleList)
        if (collections.isEmpty() && bookSource.bookUrlPattern.isNullOrEmpty()) {
            getInfoItem(analyzer, bookSource, baseUrl)?.let { searchBook ->
                searchBook.bookInfoHtml = body
                bookList.add(searchBook)
            }
        } else {
            if (allInOne) {
                for (item in collections) {
                    getAllInOneItem(item, analyzer, bookListRule, bookSource, baseUrl)?.let { searchBook ->
                        if (baseUrl == searchBook.bookUrl) {
                            searchBook.bookInfoHtml = body
                        }
                        bookList.add(searchBook)
                    }
                }
            } else {
                for (item in collections) {
                    getSearchItem(item, analyzer, bookListRule, bookSource, baseUrl)?.let { searchBook ->
                        if (baseUrl == searchBook.bookUrl) {
                            searchBook.bookInfoHtml = body
                        }
                        bookList.add(searchBook)
                    }
                }
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
                analyzeRule.setContent(analyzeRule.getElement(it))
            }
            searchBook.name = analyzeRule.getString(name ?: "")
            if (!searchBook.name.isNullOrEmpty()) {
                searchBook.author = analyzeRule.getString(author ?: "")
                searchBook.coverUrl = analyzeRule.getString(coverUrl ?: "")
                searchBook.intro = analyzeRule.getString(intro ?: "")
                searchBook.latestChapterTitle = analyzeRule.getString(lastChapter ?: "")
                searchBook.kind = analyzeRule.getString(kind ?: "")
                return searchBook
            }
        }
        return null
    }

    private fun getAllInOneItem(
        item: Any,
        analyzeRule: AnalyzeRule,
        bookListRule: BookListRule,
        bookSource: BookSource,
        baseUrl: String
    ): SearchBook? {
        val searchBook = SearchBook()
        searchBook.origin = bookSource.bookSourceUrl
        searchBook.originName = bookSource.bookSourceName
        val nativeObject = item as NativeObject
        searchBook.name = nativeObject[bookListRule.name]?.toString()
        searchBook.name?.let {
            searchBook.author = nativeObject[bookListRule.author]?.toString()
            searchBook.coverUrl = nativeObject[bookListRule.coverUrl]?.toString()
            searchBook.intro = nativeObject[bookListRule.intro]?.toString()
            searchBook.kind = nativeObject[bookListRule.kind]?.toString()

            return searchBook
        }
        return null
    }

    private fun getSearchItem(
        item: Any,
        analyzeRule: AnalyzeRule,
        bookListRule: BookListRule,
        bookSource: BookSource,
        baseUrl: String
    ): SearchBook? {
        val searchBook = SearchBook()
        searchBook.origin = bookSource.bookSourceUrl
        searchBook.originName = bookSource.bookSourceName
        analyzeRule.setBook(searchBook)
        analyzeRule.setContent(item)
        searchBook.name = analyzeRule.getString(bookListRule.name ?: "")
        searchBook.name?.let {
            searchBook.author = analyzeRule.getString(bookListRule.author ?: "")
            searchBook.kind = analyzeRule.getString(bookListRule.kind ?: "")
            searchBook.intro = analyzeRule.getString(bookListRule.intro ?: "")

            return searchBook
        }
        return null
    }
}