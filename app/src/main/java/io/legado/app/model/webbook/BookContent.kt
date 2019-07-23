package io.legado.app.model.webbook

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.model.analyzeRule.AnalyzeUrl
import retrofit2.Response

object BookContent {

    fun analyzeContent(
        response: Response<String>,
        book: Book,
        bookChapter: BookChapter,
        bookSource: BookSource,
        analyzeUrl: AnalyzeUrl
    ): String {


        return ""
    }

}