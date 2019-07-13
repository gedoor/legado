package io.legado.app.model.webbook

import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.NetworkUtils
import retrofit2.Response

class BookList {

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
                getItem(analyzer, bookSource)?.let { searchBook ->
                    searchBook.bookInfoHtml = body
                    bookList.add(searchBook)
                }
                return bookList
            }
        }
        return bookList
    }

    private fun getItem(analyzeRule: AnalyzeRule, bookSource: BookSource): SearchBook? {
        val searchBook = SearchBook()
        analyzeRule.setBook(searchBook)
        bookSource.getBookInfoRule().init?.let {
            analyzeRule.setContent(analyzeRule.getElement(it))
        }

        return null
    }
}