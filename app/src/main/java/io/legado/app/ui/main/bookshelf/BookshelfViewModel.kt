package io.legado.app.ui.main.bookshelf

import android.app.Application
import com.google.gson.stream.JsonWriter
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.http.newCallResponseBody
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.text
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

class BookshelfViewModel(application: Application) : BaseViewModel(application) {

    fun addBookByUrl(bookUrls: String) {
        var successCount = 0
        execute {
            val hasBookUrlPattern: List<BookSource> by lazy {
                appDb.bookSourceDao.hasBookUrlPattern
            }
            val urls = bookUrls.split("\n")
            for (url in urls) {
                val bookUrl = url.trim()
                if (bookUrl.isEmpty()) continue
                if (appDb.bookDao.getBook(bookUrl) != null) continue
                val baseUrl = NetworkUtils.getBaseUrl(bookUrl) ?: continue
                var source = appDb.bookSourceDao.getBookSource(baseUrl)
                if (source == null) {
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
                            it.order = appDb.bookDao.minOrder - 1
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

    fun exportBookshelf(books: List<Book>?, success: (file: File) -> Unit) {
        execute {
            books?.let {
                val path = "${context.filesDir}/books.json"
                FileUtils.delete(path)
                val file = FileUtils.createFileWithReplace(path)
                @Suppress("BlockingMethodInNonBlockingContext")
                FileOutputStream(file).use { out ->
                    val writer = JsonWriter(OutputStreamWriter(out, "UTF-8"))
                    writer.setIndent("  ")
                    writer.beginArray()
                    books.forEach {
                        val bookMap = hashMapOf<String, String?>()
                        bookMap["name"] = it.name
                        bookMap["author"] = it.author
                        bookMap["intro"] = it.getDisplayIntro()
                        GSON.toJson(bookMap, bookMap::class.java, writer)
                    }
                    writer.endArray()
                    writer.close()
                }
                file
            } ?: throw NoStackTraceException("书籍不能为空")
        }.onSuccess {
            success(it)
        }.onError {
            context.toastOnUi("导出书籍出错\n${it.localizedMessage}")
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
            GSON.fromJsonArray<Map<String, String?>>(json).getOrThrow()?.forEach { bookInfo ->
                if (!isActive) return@execute
                val name = bookInfo["name"] ?: ""
                val author = bookInfo["author"] ?: ""
                if (name.isNotEmpty() && appDb.bookDao.getBook(name, author) == null) {
                    WebBook.preciseSearch(this, bookSources, name, author)
                        .onSuccess {
                            val book = it.first
                            if (groupId > 0) {
                                book.group = groupId
                            }
                            book.save()
                        }.onError { e ->
                            context.toastOnUi(e.localizedMessage)
                        }
                }
            }
        }.onError {
            it.printOnDebug()
        }.onFinally {
            context.toastOnUi(R.string.success)
        }
    }

}
