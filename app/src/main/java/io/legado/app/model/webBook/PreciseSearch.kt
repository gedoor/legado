package io.legado.app.model.webBook

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive

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
            kotlin.runCatching {
                if (!scope.isActive) return null
                WebBook.searchBookAwait(scope, bookSource, name).firstOrNull {
                    it.name == name && it.author == author
                }?.let {
                    return if (it.tocUrl.isBlank()) {
                        if (!scope.isActive) return null
                        WebBook.getBookInfoAwait(scope, bookSource, it.toBook())
                    } else {
                        it.toBook()
                    }
                }
            }
        }
        return null
    }

}