package io.legado.app.ui.book.toc


import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book

class ChapterListViewModel(application: Application) : BaseViewModel(application) {
    var bookUrl: String = ""
    var book: Book? = null
    var chapterCallBack: ChapterListCallBack? = null
    var bookMarkCallBack: BookmarkCallBack? = null

    fun initBook(bookUrl: String, success: () -> Unit) {
        this.bookUrl = bookUrl
        execute {
            book = App.db.bookDao.getBook(bookUrl)
        }.onSuccess {
            success.invoke()
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