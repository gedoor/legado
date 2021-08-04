package io.legado.app.ui.book.group

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookGroup

class GroupViewModel(application: Application) : BaseViewModel(application) {

    fun addGroup(groupName: String) {
        execute {
            var id = 1L
            val idsSum = appDb.bookGroupDao.idsSum
            while (id and idsSum != 0L) {
                id = id.shl(1)
            }
            val bookGroup = BookGroup(
                groupId = id,
                groupName = groupName,
                order = appDb.bookGroupDao.maxOrder.plus(1)
            )
            appDb.bookGroupDao.insert(bookGroup)
        }
    }

    fun upGroup(vararg bookGroup: BookGroup) {
        execute {
            appDb.bookGroupDao.update(*bookGroup)
        }
    }

    fun delGroup(vararg bookGroup: BookGroup) {
        execute {
            appDb.bookGroupDao.delete(*bookGroup)
            bookGroup.forEach { group ->
                val books = appDb.bookDao.getBooksByGroup(group.groupId)
                books.forEach {
                    it.group = it.group - group.groupId
                }
                appDb.bookDao.update(*books.toTypedArray())
            }
        }
    }


}