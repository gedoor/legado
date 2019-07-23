package io.legado.app.model.webbook

import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import retrofit2.Response

object BookContent {

    @Throws(Exception::class)
    suspend fun analyzeContent(
        coroutineScope: CoroutineScope,
        response: Response<String>,
        book: Book,
        bookChapter: BookChapter,
        bookSource: BookSource,
        analyzeUrl: AnalyzeUrl
    ): String {
        val baseUrl: String = NetworkUtils.getUrl(response)
        val body: String? = response.body()
        body ?: throw Exception(
            App.INSTANCE.getString(
                R.string.get_web_content_error,
                baseUrl
            )
        )
        val contentRule = bookSource.getContentRule()

        return ""
    }

    fun analyzeContent(
        body: String,
        contentRule: ContentRule
    ): ContentData<String> {

        return ContentData("", "")
    }
}