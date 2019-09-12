package io.legado.app.ui.book.search

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.WebBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

class SearchViewModel(application: Application) : BaseViewModel(application) {
    private var task: Coroutine<*>? = null
    var searchKey: String = ""
    var startTime: Long = 0
    var searchPage = 0

    fun search(
        key: String,
        start: (() -> Unit)? = null,
        finally: (() -> Unit)? = null
    ) {
        if (key.isEmpty()) return
        task?.cancel()
        searchKey = key
        startTime = System.currentTimeMillis()
        start?.invoke()
        task = execute {
            //onCleared时自动取消
            val bookSourceList = App.db.bookSourceDao().allEnabled
            for (item in bookSourceList) {
                //task取消时自动取消 by （scope = this@execute）
                WebBook(item).searchBook(key, searchPage, scope = this@execute)
                    .timeout(30000L)
                    .onSuccess(Dispatchers.IO) {
                        it?.let { list ->
                            list.map { searchBook ->
                                if (searchBook.name.contains(key) || searchBook.author.contains(key))
                                    App.db.searchBookDao().insert(searchBook)
                            }
                        }
                    }
                delay(100)//每隔100毫秒搜索一个书源
            }
        }.onError {
            it.printStackTrace()
        }

        task?.invokeOnCompletion {
            finally?.invoke()
        }
    }

    fun stop() {
        task?.cancel()
    }

    fun getSearchBook(name: String, author: String, success: ((searchBook: SearchBook?) -> Unit)?) {
        execute {
            val searchBook = App.db.searchBookDao().getFirstByNameAuthor(name, author)
            success?.invoke(searchBook)
        }
    }
}
