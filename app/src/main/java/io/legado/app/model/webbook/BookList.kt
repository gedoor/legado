package io.legado.app.model.webbook

import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import retrofit2.Response

class BookList {

    fun analyzeBookList(response: Response<String>, bookSource: BookSource): ArrayList<SearchBook> {
        var bookList = ArrayList<SearchBook>()

        return bookList
    }
}