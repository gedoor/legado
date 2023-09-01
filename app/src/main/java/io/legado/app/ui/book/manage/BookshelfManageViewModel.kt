package io.legado.app.ui.book.manage

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppLog
import io.legado.app.constant.BookType
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.book.isLocal
import io.legado.app.help.book.removeType
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.localBook.LocalBook
import io.legado.app.model.webBook.WebBook
import kotlinx.coroutines.delay


class BookshelfManageViewModel(application: Application) : BaseViewModel(application) {
    var groupId: Long = -1L
    var groupName: String? = null
    val batchChangeSourceState = MutableLiveData<Boolean>()
    val batchChangeSourceProcessLiveData = MutableLiveData<String>()
    var batchChangeSourceCoroutine: Coroutine<Unit>? = null

    fun upCanUpdate(books: List<Book>, canUpdate: Boolean) {
        execute {
            val array = Array(books.size) {
                books[it].copy(canUpdate = canUpdate)
            }
            appDb.bookDao.update(*array)
        }
    }

    fun updateBook(vararg book: Book) {
        execute {
            appDb.bookDao.update(*book)
        }
    }

    fun deleteBook(books: List<Book>, deleteOriginal: Boolean = false) {
        execute {
            appDb.bookDao.delete(*books.toTypedArray())
            books.forEach {
                if (it.isLocal) {
                    LocalBook.deleteBook(it, deleteOriginal)
                }
            }
        }
    }

    fun changeSource(books: List<Book>, source: BookSource) {
        batchChangeSourceCoroutine?.cancel()
        batchChangeSourceCoroutine = execute {
            val changeSourceDelay = AppConfig.batchChangeSourceDelay * 1000L
            books.forEachIndexed { index, book ->
                batchChangeSourceProcessLiveData.postValue("${index + 1} / ${books.size}")
                if (book.isLocal) return@forEachIndexed
                if (book.origin == source.bookSourceUrl) return@forEachIndexed
                val newBook = WebBook.preciseSearchAwait(this, source, book.name, book.author)
                    .onFailure {
                        AppLog.put("获取书籍出错\n${it.localizedMessage}", it, true)
                    }.getOrNull() ?: return@forEachIndexed
                WebBook.getChapterListAwait(source, newBook)
                    .onFailure {
                        AppLog.put("获取目录出错\n${it.localizedMessage}", it, true)
                    }.getOrNull()?.let { toc ->
                        book.migrateTo(newBook, toc)
                        book.removeType(BookType.updateError)
                        appDb.bookDao.insert(newBook)
                        appDb.bookChapterDao.insert(*toc.toTypedArray())
                    }
                delay(changeSourceDelay)
            }
        }.onStart {
            batchChangeSourceState.postValue(true)
        }.onFinally {
            batchChangeSourceState.postValue(false)
        }
    }

}