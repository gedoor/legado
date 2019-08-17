package io.legado.app.ui.bookinfo

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter

class BookInfoViewModel(application: Application) : BaseViewModel(application) {

    val bookData = MutableLiveData<Book>()
    val chapterListData = MutableLiveData<List<BookChapter>>()
    val isLoadingData = MutableLiveData<Boolean>()
    var inBookshelf = false

    fun loadBook(intent: Intent) {
        execute {
            intent.getStringExtra("bookUrl")?.let {
                App.db.bookDao().getBook(it)?.let { book ->
                    inBookshelf = true
                    bookData.postValue(book)
                    val chapterList = App.db.bookChapterDao().getChapterList(it)
                    if (chapterList.isNotEmpty()) {
                        chapterListData.postValue(chapterList)
                    } else {
                        loadChapter(book)
                    }
                }
            } ?: intent.getStringExtra("searchBookUrl")?.let {
                App.db.searchBookDao().getSearchBook(it)?.toBook()?.let { book ->
                    bookData.postValue(book)
                    if (book.tocUrl.isEmpty()) {
                        loadBookInfo()
                    } else {
                        loadChapter(book)
                    }
                }
            }
        }
    }

    fun loadBookInfo() {
        isLoadingData.postValue(false)
    }

    fun loadChapter(book: Book) {
        isLoadingData.postValue(false)
        App.db.bookSourceDao().getBookSource(book.origin)?.let {

        }
    }

    fun saveBook(success: (() -> Unit)?) {
        execute {
            bookData.value?.let { book ->
                App.db.bookDao().insert(book)
            }
            chapterListData.value?.let {
                App.db.bookChapterDao().insert(*it.toTypedArray())
            }
        }.onSuccess {
            success?.invoke()
        }
    }
}