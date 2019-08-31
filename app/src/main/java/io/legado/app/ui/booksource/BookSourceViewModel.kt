package io.legado.app.ui.booksource

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.BookSource

class BookSourceViewModel(application: Application) : BaseViewModel(application) {


    fun topSource(bookSource: BookSource) {
        execute {
            val minXh = App.db.bookSourceDao().minOrder
            bookSource.customOrder = minXh - 1
            App.db.bookSourceDao().insert(bookSource)
        }
    }

    fun del(bookSource: BookSource) {
        execute { App.db.bookSourceDao().delete(bookSource) }
    }

    fun update(vararg bookSource: BookSource) {
        execute { App.db.bookSourceDao().update(*bookSource) }
    }

    fun upGroup(oldGroup: String, newGroup: String?) {

    }

    fun delGroup(group: String) {

    }
}