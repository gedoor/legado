package io.legado.app.model.webbook

import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl

object BookInfo {

    @Throws(Exception::class)
    fun analyzeBookInfo(
        book: Book,
        body: String?,
        bookSource: BookSource,
        analyzeUrl: AnalyzeUrl,
        isSearch: Boolean = true
    ) {
        val baseUrl = analyzeUrl.url
        body ?: throw Exception(
            App.INSTANCE.getString(
                R.string.get_web_content_error,
                baseUrl
            )
        )
        val infoRule = bookSource.getBookInfoRule()
        val analyzeRule = AnalyzeRule(book)
        analyzeRule.setContent(body)
        infoRule.init?.let {
            if (it.isNotEmpty()) {
                analyzeRule.setContent(analyzeRule.getElement(it))
            }
        }
        analyzeRule.getString(infoRule.name ?: "")?.let {
            if (it.isNotEmpty()) book.name = it
        }
        analyzeRule.getString(infoRule.author ?: "")?.let {
            if (it.isNotEmpty()) book.author = it
        }
        analyzeRule.getString(infoRule.kind ?: "")?.let {
            if (it.isNotEmpty()) book.kind = it
        }
        analyzeRule.getString(infoRule.intro ?: "")?.let {
            if (it.isNotEmpty()) book.intro = it
        }

    }

}