package io.legado.app.ui.main.bookshelf

import android.app.Application
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.BookSource
import io.legado.app.model.webBook.PreciseSearch
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.isActive
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
                    WebBook(bookSource).getBookInfo(this, book)
                        .onSuccess(IO) {
                            it.order = appDb.bookDao.maxOrder + 1
                            appDb.bookDao.insert(it)
                            successCount++
                        }.onError {
                            throw Exception(it.localizedMessage)
                        }
                }
            }
        }.onSuccess {
            if (successCount > 0) {
                toastOnUi(R.string.success)
            } else {
                toastOnUi("ERROR")
            }
        }.onError {
            toastOnUi(it.localizedMessage ?: "ERROR")
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
                    RxHttp.get(text).toText().await().let {
                        importBookshelf(it, groupId)
                    }
                }
                text.isJsonArray() -> {
                    importBookshelfByJson(text, groupId)
                }
                else -> {
                    throw Exception("格式不对")
                }
            }
        }.onError {
            toastOnUi(it.localizedMessage ?: "ERROR")
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
                    val book = PreciseSearch
                        .searchFirstBook(this, bookSources, name, author)
                    book?.let {
                        if (groupId > 0) {
                            book.group = groupId
                        }
                        appDb.bookDao.insert(book)
                    }
                }
            }
        }.onFinally {
            toastOnUi(R.string.success)
        }
    }

    fun checkGroup(groups: List<BookGroup>) {
        groups.forEach { group ->
            if (group.groupId >= 0 && group.groupId and (group.groupId - 1) != 0L) {
                var id = 1L
                val idsSum = appDb.bookGroupDao.idsSum
                while (id and idsSum != 0L) {
                    id = id.shl(1)
                }
                appDb.bookGroupDao.delete(group)
                appDb.bookGroupDao.insert(group.copy(groupId = id))
                appDb.bookDao.upGroup(group.groupId, id)
            }
        }
    }

}
