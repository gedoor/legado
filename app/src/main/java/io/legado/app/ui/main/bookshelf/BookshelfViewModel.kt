package io.legado.app.ui.main.bookshelf

import android.app.Application
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.BookSource
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toText

class BookshelfViewModel(application: Application) : BaseViewModel(application) {

    fun addBookByUrl(bookUrls: String) {
        var successCount = 0
        execute {
            var hasBookUrlPattern: List<BookSource>? = null
            val urls = bookUrls.split("\n")
            for (url in urls) {
                val bookUrl = url.trim()
                if (bookUrl.isEmpty()) continue
                if (App.db.bookDao.getBook(bookUrl) != null) continue
                val baseUrl = NetworkUtils.getBaseUrl(bookUrl) ?: continue
                var source = App.db.bookSourceDao.getBookSource(baseUrl)
                if (source == null) {
                    if (hasBookUrlPattern == null) {
                        hasBookUrlPattern = App.db.bookSourceDao.hasBookUrlPattern
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
                    WebBook(bookSource).getBookInfo(this, book)
                        .onSuccess(IO) {
                            it.order = App.db.bookDao.maxOrder + 1
                            App.db.bookDao.insert(it)
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

    fun exportBookshelf(books: List<Book>?, success: (json: String) -> Unit) {
        execute {
            val exportList = arrayListOf<Map<String, String?>>()
            books?.forEach {
                val bookMap = hashMapOf<String, String?>()
                bookMap["name"] = it.name
                bookMap["author"] = it.author
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
                    RxHttp.get(text).toText().await().let {
                        importBookshelf(it, groupId)
                    }
                }
                text.isJsonArray() -> {
                    val bookSources = App.db.bookSourceDao.allEnabled
                    GSON.fromJsonArray<Map<String, String?>>(text)?.forEach {
                        val name = it["name"] ?: ""
                        val author = it["author"] ?: ""
                        bookSources.forEach { bookSource ->
                            runCatching {
                                val webBook = WebBook(bookSource)
                                val searchBooks = webBook.searchBookAwait(this, name)
                                val searchBook = searchBooks.firstOrNull()
                                if (searchBook != null && searchBook.name == name && searchBook.author == author) {
                                    val book = webBook.getBookInfoAwait(this, searchBook.toBook())
                                    App.db.bookDao.insert(book)
                                }
                            }
                        }
                    }
                }
                else -> {
                    throw Exception("格式不对")
                }
            }
        }.onError {
            toast(it.localizedMessage ?: "ERROR")
        }
    }

    fun checkGroup(groups: List<BookGroup>) {
        groups.forEach { group ->
            if (group.groupId >= 0 && group.groupId and (group.groupId - 1) != 0L) {
                var id = 1L
                val idsSum = App.db.bookGroupDao.idsSum
                while (id and idsSum != 0L) {
                    id = id.shl(1)
                }
                App.db.bookGroupDao.delete(group)
                App.db.bookGroupDao.insert(group.copy(groupId = id))
                App.db.bookDao.upGroup(group.groupId, id)
            }
        }
    }

}
