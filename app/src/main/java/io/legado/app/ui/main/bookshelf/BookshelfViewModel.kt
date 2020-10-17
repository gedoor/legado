package io.legado.app.ui.main.bookshelf

import android.app.Application
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.BookSource
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers.IO

class BookshelfViewModel(application: Application) : BaseViewModel(application) {

    fun addBookByUrl(bookUrls: String) {
        var successCount = 0
        execute {
            var hasBookUrlPattern: List<BookSource>? = null
            val urls = bookUrls.split("\n")
            for (url in urls) {
                val bookUrl = url.trim()
                if (bookUrl.isEmpty()) continue
                if (App.db.bookDao().getBook(bookUrl) != null) continue
                val baseUrl = NetworkUtils.getBaseUrl(bookUrl) ?: continue
                var source = App.db.bookSourceDao().getBookSource(baseUrl)
                if (source == null) {
                    if (hasBookUrlPattern == null) {
                        hasBookUrlPattern = App.db.bookSourceDao().hasBookUrlPattern
                    }
                    hasBookUrlPattern.forEach { bookSource ->
                        if (bookUrl.matches(bookSource.bookUrlPattern!!.toRegex())) {
                            source = bookSource
                            return@forEach
                        }
                    }
                }
                source?.let { bookSource ->
                    val book = Book(
                        bookUrl = bookUrl,
                        origin = bookSource.bookSourceUrl,
                        originName = bookSource.bookSourceName
                    )
                    WebBook(bookSource).getBookInfo(book, this)
                        .onSuccess(IO) {
                            it.order = App.db.bookDao().maxOrder + 1
                            App.db.bookDao().insert(it)
                            successCount++
                        }.onError {
                            throw Exception(it.localizedMessage)
                        }
                }
            }
        }.onSuccess {
            if (successCount > 0) {
                toast(R.string.success)
            } else {
                toast("ERROR")
            }
        }.onError {
            toast(it.localizedMessage ?: "ERROR")
        }
    }

    fun checkGroup(groups: List<BookGroup>) {
        groups.forEach { group ->
            if (group.groupId >= 0 && group.groupId and (group.groupId - 1) != 0L) {
                var id = 1L
                val idsSum = App.db.bookGroupDao().idsSum
                while (id and idsSum != 0L) {
                    id = id.shl(1)
                }
                App.db.bookGroupDao().delete(group)
                App.db.bookGroupDao().insert(group.copy(groupId = id))
                App.db.bookDao().upGroup(group.groupId, id)
            }
        }
    }

}
