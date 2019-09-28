package io.legado.app.ui.main.bookshelf

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.BookGroup

class BookshelfViewModel(application: Application) : BaseViewModel(application) {


    fun addGroup(groupName: String) {
        execute {
            val maxId = App.db.bookGroupDao().maxId
            val bookGroup = BookGroup(
                groupId = maxId.plus(1),
                groupName = groupName,
                order = maxId.plus(1)
            )
            App.db.bookGroupDao().insert(bookGroup)
        }
    }

    fun upGroup(vararg bookGroup: BookGroup) {
        execute {
            App.db.bookGroupDao().update(*bookGroup)
        }
    }

    fun delGroup(vararg bookGroup: BookGroup) {
        execute {
            App.db.bookGroupDao().delete(*bookGroup)
        }
    }
}
