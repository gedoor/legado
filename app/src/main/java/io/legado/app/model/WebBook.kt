package io.legado.app.model

import io.legado.app.data.api.IHttpGetApi
import io.legado.app.data.api.IHttpPostApi
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.http.HttpHelper
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.webbook.BookList

class WebBook(private val bookSource: BookSource) {

    fun searchBook(key: String, page: Int?): Coroutine<List<SearchBook>> {
        return Coroutine.async {
            bookSource.getSearchRule().searchUrl?.let { searchUrl ->
                val analyzeUrl = AnalyzeUrl(searchUrl)
                val response = when {
                    analyzeUrl.method == AnalyzeUrl.Method.POST -> HttpHelper.getApiService<IHttpPostApi>(
                        analyzeUrl.baseUrl
                    ).postBody(
                        analyzeUrl.url,
                        analyzeUrl.body,
                        analyzeUrl.headerMap
                    ).await()
                    analyzeUrl.fieldMap.isEmpty() -> HttpHelper.getApiService<IHttpGetApi>(
                        analyzeUrl.baseUrl
                    )[analyzeUrl.url, analyzeUrl.headerMap].await()
                    else -> HttpHelper.getApiService<IHttpGetApi>(analyzeUrl.baseUrl)
                        .getMap(analyzeUrl.url, analyzeUrl.fieldMap, analyzeUrl.headerMap).await()
                }
                return@async BookList.analyzeBookList(response, bookSource, analyzeUrl)
            }
            return@async arrayListOf<SearchBook>()
        }
    }


}