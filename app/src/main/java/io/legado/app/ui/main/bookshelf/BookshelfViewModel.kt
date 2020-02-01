package io.legado.app.ui.main.bookshelf

import android.app.Application
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.BookSource
import io.legado.app.model.WebBook
import io.legado.app.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers.IO

class BookshelfViewModel(application: Application) : BaseViewModel(application) {


    fun addGroup(groupName: String) {
        execute {
            val bookGroup = BookGroup(
                groupId = App.db.bookGroupDao().maxId.plus(1),
                groupName = groupName,
                order = App.db.bookGroupDao().maxOrder.plus(1)
            )
            App.db.bookGroupDao().insert(bookGroup)
        }
    }

    fun upGroup(vararg bookGroup: BookGroup) {
        execute {
            App.db.bookGroupDao().update(*bookGroup)
        }
    }

    fun delGroup(vararg bookGroup: BookGroup) {
        execute {
            App.db.bookGroupDao().delete(*bookGroup)
        }
    }

    fun addBookByUrl(bookUrls: String) {
        execute {
            var hasBookUrlPattern: List<BookSource>? = null
            val urls = bookUrls.split("\n")
            for (url in urls) {
                val bookUrl = url.trim()
                if (bookUrl.isEmpty()) continue
                App.db.bookDao().getBook(bookUrl) ?: continue
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
                    val book = Book(bookUrl = bookUrl)
                    WebBook(bookSource).getBookInfo(book, this)
                        .onSuccess(IO) {
                            it?.let { book ->
                                App.db.bookDao().insert(book)
                            }
                        }
                }
            }
        }.onSuccess {
            toast(R.string.success)
        }.onError {
            toast(it.localizedMessage ?: "ERROR")
        }
    }

}
