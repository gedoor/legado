package io.legado.app.ui.bookinfo

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book

class BookInfoViewModel(application: Application) : BaseViewModel(application) {

    val bookData = MutableLiveData<Book>()
    val isLoadingData = MutableLiveData<Boolean>()
    var inBookshelf = false

    fun loadBook(intent: Intent) {
        execute {
            intent.getStringExtra("bookUrl")?.let {
                App.db.bookDao().getBook(it)?.let { book ->
                    inBookshelf = true
                    bookData.postValue(book)
                }
            } ?: intent.getStringExtra("searchBookUrl")?.let {
                App.db.searchBookDao().getSearchBook(it)?.toBook()?.let { book ->
                    bookData.postValue(book)
                    if (book.tocUrl.isNullOrEmpty()) {
                        loadBookInfo()
                    } else {
                        loadChapter()
                    }
                }
            }
        }
    }

    fun loadBookInfo() {

    }

    fun loadChapter() {

    }

}