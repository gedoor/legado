package io.legado.app.ui.book.toc


import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book

class ChapterListViewModel(application: Application) : BaseViewModel(application) {
    var bookUrl: String = ""
    var bookData = MutableLiveData<Book>()
    var chapterCallBack: ChapterListCallBack? = null
    var bookMarkCallBack: BookmarkCallBack? = null

    fun initBook(bookUrl: String) {
        this.bookUrl = bookUrl
        execute {
            appDb.bookDao.getBook(bookUrl)?.let {
                bookData.postValue(it)
            }
        }
    }

    fun startChapterListSearch(newText: String?) {
        chapterCallBack?.startChapterListSearch(newText)
    }

    fun startBookmarkSearch(newText: String?) {
        bookMarkCallBack?.startBookmarkSearch(newText)
    }

    interface ChapterListCallBack {
        fun startChapterListSearch(newText: String?)
    }

    interface BookmarkCallBack {
        fun startBookmarkSearch(newText: String?)
    }
}