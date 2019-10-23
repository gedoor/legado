package io.legado.app.ui.chapterlist


import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book

class ChapterListViewModel(application: Application) : BaseViewModel(application) {

    var bookDate = MutableLiveData<Book>()
    var bookUrl: String? = null

    fun loadBook() {
        execute {
            bookUrl?.let {
                App.db.bookDao().getBook(it)?.let { book ->
                    bookDate.postValue(book)
                }
            }
        }
    }
}