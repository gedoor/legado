package io.legado.app.ui.book.search

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.SearchKeyword
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.WebBook
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class SearchViewModel(application: Application) : BaseViewModel(application) {
    private var searchPool = Executors.newFixedThreadPool(16).asCoroutineDispatcher()
    private var task: Coroutine<*>? = null
    var searchKey: String = ""
    var startTime: Long = 0
    var searchPage = 0

    fun search(
        key: String,
        start: (() -> Unit)? = null,
        finally: (() -> Unit)? = null
    ) {
        task?.cancel()
        if (key.isEmpty()) {
            searchPage++
        } else {
            searchKey = key
        }
        if (searchKey.isEmpty()) return
        startTime = System.currentTimeMillis()
        start?.invoke()
        task = execute {
            //onCleared时自动取消
            val searchGroup = context.getPrefString("searchGroup") ?: ""
            val bookSourceList = if (searchGroup.isBlank()) {
                App.db.bookSourceDao().allEnabled
            } else {
                App.db.bookSourceDao().getEnabledByGroup(searchGroup)
            }
            for (item in bookSourceList) {
                //task取消时自动取消 by （scope = this@execute）
                WebBook(item).searchBook(
                    searchKey,
                    searchPage,
                    scope = this@execute,
                    context = searchPool
                )
                    .timeout(30000L)
                    .onSuccess(Dispatchers.IO) {
                        it?.let { list ->
                            searchSuccess(list)
                        }
                    }
            }
        }

        task?.invokeOnCompletion {
            finally?.invoke()
        }
    }

    private fun searchSuccess(searchBooks: List<SearchBook>) {
        searchBooks.forEach { searchBook ->
            if (context.getPrefBoolean("precisionSearch")) {
                if (searchBook.name.contains(searchKey)
                    || searchBook.author.contains(searchKey)
                ) App.db.searchBookDao().insert(searchBook)
            } else
                App.db.searchBookDao().insert(searchBook)
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

    fun saveSearchKey(key: String) {
        execute {
            App.db.searchKeywordDao().get(key)?.let {
                it.usage = it.usage + 1
                App.db.searchKeywordDao().update(it)
            } ?: App.db.searchKeywordDao().insert(SearchKeyword(key, 1))
        }
    }

    fun clearHistory() {
        execute {
            App.db.searchKeywordDao().deleteAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchPool.close()
    }
}
