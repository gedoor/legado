package io.legado.app.ui.book.group

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.BookGroup

class GroupViewModel(application: Application) : BaseViewModel(application) {

    fun addGroup(groupName: String) {
        execute {
            val bookGroup = BookGroup(
                groupId = App.db.bookGroupDao().maxId.plus(1),
                groupName = groupName,
                order = App.db.bookGroupDao().maxOrder.plus(1)
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