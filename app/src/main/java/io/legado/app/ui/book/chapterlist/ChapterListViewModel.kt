package io.legado.app.ui.book.chapterlist


import android.app.Application
import io.legado.app.base.BaseViewModel

class ChapterListViewModel(application: Application) : BaseViewModel(application) {
    var bookUrl: String = ""
    var chapterCallBack: ChapterListCallBack? = null
    var bookMarkCallBack: BookmarkCallBack? = null

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