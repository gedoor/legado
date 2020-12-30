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
                    WebBook(bookSource).getBookInfo(book, this)
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
                bookMap["bookUrl"] = it.bookUrl
                bookMap["coverUrl"] = it.coverUrl
                bookMap["tocUrl"] = it.tocUrl
                bookMap["kind"] = it.kind
                bookMap["intro"] = it.getDisplayIntro()
                bookMap["origin"] = it.origin
                bookMap["originName"] = it.originName
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
                    GSON.fromJsonArray<Map<String, String?>>(text)?.forEach {
                        val book = Book(
                            bookUrl = it["bookUrl"] ?: "",
                            name = it["name"] ?: "",
                            author = it["author"] ?: "",
                            coverUrl = it["coverUrl"],
                            tocUrl = it["tocUrl"] ?: "",
                            kind = it["kind"],
                            intro = it["intro"] ?: "",
                            origin = it["origin"] ?: "",
                            originName = it["originName"] ?: ""
                        )
                        if (groupId > 0) {
                            book.group = groupId
                        }
                        if (App.db.bookDao.getBook(book.name, book.author) == null) {
                            App.db.bookDao.insert(book)
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
