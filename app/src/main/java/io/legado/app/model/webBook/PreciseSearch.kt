package io.legado.app.model.webBook

import io.legado.app.data.entities.BookSource
import kotlinx.coroutines.CoroutineScope

/**
 * 精准搜索
 */
object PreciseSearch {

    suspend fun searchFirstBook(
        scope: CoroutineScope,
        bookSources: List<BookSource>,
        name: String,
        author: String
    ) {

    }

}