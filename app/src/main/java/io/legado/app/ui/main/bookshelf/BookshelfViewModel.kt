package io.legado.app.ui.main.bookshelf

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.BookGroup

class BookshelfViewModel(application: Application) : BaseViewModel(application) {

    fun saveBookGroup(group: String?) {
        if (!group.isNullOrBlank()) {
            execute {
                App.db.bookGroupDao().insert(
                    BookGroup(
                        App.db.bookGroupDao().maxId + 1,
                        group
                    )
                )
            }
        }
    }

    fun addGroup(groupName: String) {

    }

    fun upGroup(bookGroup: BookGroup) {

    }

    fun delGroup(bookGroup: BookGroup) {

    }
}
