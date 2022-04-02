package io.legado.app.ui.book.bookmark

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Bookmark

class AllBookmarkViewModel(application: Application) : BaseViewModel(application) {


    fun initData(onSuccess: (bookmarks: List<Bookmark>) -> Unit) {
        execute {
            appDb.bookmarkDao.all
        }.onSuccess {
            onSuccess.invoke(it)
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        execute {
            appDb.bookmarkDao.delete(bookmark)
        }
    }

}