package io.legado.app.ui.booksource

import android.app.Application
import android.text.TextUtils
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.BookSource
import io.legado.app.utils.splitNotBlank

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
        execute {
            val sources = App.db.bookSourceDao().getByGroup(oldGroup)
            sources.map { source ->
                source.bookSourceGroup?.splitNotBlank(",")?.toHashSet()?.let {
                    it.remove(oldGroup)
                    if (!newGroup.isNullOrEmpty())
                        it.add(newGroup)
                    source.bookSourceGroup = TextUtils.join(",", it)
                }
            }
            App.db.bookSourceDao().update(*sources.toTypedArray())
        }
    }

    fun delGroup(group: String) {
        execute {
            execute {
                val sources = App.db.bookSourceDao().getByGroup(group)
                sources.map { source ->
                    source.bookSourceGroup?.splitNotBlank(",")?.toHashSet()?.let {
                        it.remove(group)
                        source.bookSourceGroup = TextUtils.join(",", it)
                    }
                }
                App.db.bookSourceDao().update(*sources.toTypedArray())
            }
        }
    }
}