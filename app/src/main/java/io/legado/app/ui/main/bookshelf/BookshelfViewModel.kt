package io.legado.app.ui.main.bookshelf

import android.app.Application
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.http.newCallResponseBody
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.text
import io.legado.app.model.NoStackTraceException
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.isActive

class BookshelfViewModel(application: Application) : BaseViewModel(application) {

    fun addBookByUrl(bookUrls: String) {
        var successCount = 0
        execute {
            var hasBookUrlPattern: List<BookSource>? = null
            val urls = bookUrls.split("\n")
            for (url in urls) {
                val bookUrl = url.trim()
                if (bookUrl.isEmpty()) continue
                if (appDb.bookDao.getBook(bookUrl) != null) continue
                val baseUrl = NetworkUtils.getBaseUrl(bookUrl) ?: continue
                var source = appDb.bookSourceDao.getBookSource(baseUrl)
                if (source == null) {
                    if (hasBookUrlPattern == null) {
                        hasBookUrlPattern = appDb.bookSourceDao.hasBookUrlPattern
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
                    WebBook.getBookInfo(this, bookSource, book)
                        .onSuccess(IO) {
                            it.order = appDb.bookDao.maxOrder + 1
                            it.save()
                            successCount++
                        }.onError {
                            throw it
                        }
                }
            }
        }.onSuccess {
            if (successCount > 0) {
                context.toastOnUi(R.string.success)
            } else {
                context.toastOnUi("ERROR")
            }
        }.onError {
            context.toastOnUi(it.localizedMessage ?: "ERROR")
        }
    }

    fun exportBookshelf(books: List<Book>?, success: (json: String) -> Unit) {
        execute {
            val exportList = arrayListOf<Map<String, String?>>()
            books?.forEach {
                val bookMap = hashMapOf<String, String?>()
                bookMap["name"] = it.name
                bookMap["author"] = it.author
                bookMap["intro"] = it.getDisplayIntro()
                exportList.add(bookMap)
            }
            GSON.toJson(exportList)
        }.onSuccess {
            success(it)
        }
    }

    fun importBookshelf(str: String, groupId: Long) {
        execute {
            val text = str.trim()
            when {
                text.isAbsUrl() -> {
                    okHttpClient.newCallResponseBody {
                        url(text)
                    }.text().let {
                        importBookshelf(it, groupId)
                    }
                }
                text.isJsonArray() -> {
                    importBookshelfByJson(text, groupId)
                }
                else -> {
                    throw NoStackTraceException("格式不对")
                }
            }
        }.onError {
            context.toastOnUi(it.localizedMessage ?: "ERROR")
        }
    }

    private fun importBookshelfByJson(json: String, groupId: Long) {
        execute {
            val bookSources = appDb.bookSourceDao.allEnabled
            GSON.fromJsonArray<Map<String, String?>>(json)?.forEach {
                if (!isActive) return@execute
                val name = it["name"] ?: ""
                val author = it["author"] ?: ""
                if (name.isNotEmpty() && appDb.bookDao.getBook(name, author) == null) {
                    val book = WebBook.preciseSearch(this, bookSources, name, author)?.second
                    book?.let {
                        if (groupId > 0) {
                            book.group = groupId
                        }
                        book.save()
                    }
                }
            }
        }.onFinally {
            context.toastOnUi(R.string.success)
        }
    }

}
