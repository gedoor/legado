package io.legado.app.ui.chapterlist


import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book

class ChapterListViewModel(application: Application) : BaseViewModel(application) {
    var bookUrl: String? = null
    var book: Book? = null
    var chapterCallBack: ChapterListCallBack? = null
    var bookMarkCallBack: BookmarkCallBack? = null

    fun loadBook(success: () -> Unit) {
        execute {
            bookUrl?.let {
                book = App.db.bookDao().getBook(it)
            }
        }.onSuccess {
            success()
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