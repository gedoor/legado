package io.legado.app.ui.book.toc


import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book

class TocViewModel(application: Application) : BaseViewModel(application) {
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

    fun reverseToc() {
        execute {
            val toc = appDb.bookChapterDao.getChapterList(bookUrl)
            val newToc = toc.reversed()
            newToc.forEachIndexed { index, bookChapter ->
                bookChapter.index = index
            }
            appDb.bookChapterDao.insert(*newToc.toTypedArray())
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