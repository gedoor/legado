package io.legado.app.ui.search

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.help.coroutine.CompositeCoroutine
import io.legado.app.model.WebBook

class SearchViewModel(application: Application) : BaseViewModel(application) {
    private val tasks: CompositeCoroutine = CompositeCoroutine()
    var searchPage = 0

    fun search(key: String, start: ((startTime: Long) -> Unit)? = null, finally: (() -> Unit)? = null) {
        if (key.isEmpty()) return
        tasks.clear()
        start?.invoke(System.currentTimeMillis())
        execute {
            val bookSourceList = App.db.bookSourceDao().allEnabled
            for (item in bookSourceList) {
                val search = WebBook(item).searchBook(key, searchPage)
                    .onSuccess { searchBookS ->
                        searchBookS?.let {
                            for (searchBook in searchBookS) {

                            }
                            App.db.searchBookDao().insert(*it.toTypedArray())
                        }
                    }
                tasks.add(search)
            }

        }
    }

    override fun onCleared() {
        super.onCleared()
        tasks.clear()
    }
}
