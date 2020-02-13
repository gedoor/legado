package io.legado.app.ui.book.group

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.BookGroup

class GroupViewModel(application: Application) : BaseViewModel(application) {

    fun addGroup(groupName: String) {
        execute {
            var id = 1
            val idsCount = App.db.bookGroupDao().idsCount
            while (id and idsCount != 0) {
                id *= 2
            }
            val bookGroup = BookGroup(
                groupId = id,
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