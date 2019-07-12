package io.legado.app.model.webbook

import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.model.analyzeRule.AnalyzeUrl
import retrofit2.Response

class BookList {

    @Throws(Exception::class)
    fun analyzeBookList(
        response: Response<String>,
        bookSource: BookSource,
        analyzeUrl: AnalyzeUrl,
        isSearch: Boolean = true
    ): ArrayList<SearchBook> {
        var bookList = ArrayList<SearchBook>()
        val body: String? = response.body()
        body ?: throw Exception(
            App.INSTANCE.getString(
                R.string.get_web_content_error,
                analyzeUrl.url
            )
        )

        return bookList
    }
}