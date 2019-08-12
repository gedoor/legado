package io.legado.app.ui.bookinfo

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book

class BookInfoViewModel(application: Application) : BaseViewModel(application) {

    val bookData = MutableLiveData<Book>()

    fun loadData(intent: Intent) {
        execute {
            intent.getStringExtra("bookUrl")?.let {
                App.db.bookDao().getBook(it)?.let { book ->
                    bookData.postValue(book)
                }
            } ?: intent.getStringExtra("searchBookUrl")?.let {
                App.db.searchBookDao().getSearchBook(it)?.let { searchBook ->
                    bookData.postValue(searchBook.toBook())
                }
            }
        }
    }

}