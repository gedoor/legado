package io.legado.app.model.webBook

import io.legado.app.data.entities.Book
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
    ): Book? {
        bookSources.forEach { bookSource ->
            val webBook = WebBook(bookSource)
            kotlin.runCatching {
                webBook.searchBookAwait(scope, name).firstOrNull {
                    it.name == name && it.author == author
                }?.let {
                    return if (it.tocUrl.isBlank()) {
                        webBook.getBookInfoAwait(scope, it.toBook())
                    } else {
                        it.toBook()
                    }
                }
            }
        }
        return null
    }

}