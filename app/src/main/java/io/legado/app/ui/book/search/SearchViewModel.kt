package io.legado.app.ui.book.search

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.SearchKeyword
import io.legado.app.help.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.WebBook
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefString
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class SearchViewModel(application: Application) : BaseViewModel(application) {
    private var searchPool =
        Executors.newFixedThreadPool(AppConfig.threadCount).asCoroutineDispatcher()
    private var task: Coroutine<*>? = null
    var callBack: CallBack? = null
    var searchBookLiveData = MutableLiveData<List<SearchBook>>()
    var searchKey: String = ""
    var searchPage = 1
    var isLoading = false
    private var searchBooks = arrayListOf<SearchBook>()

    fun search(key: String) {
        task?.cancel()
        if (key.isEmpty() && searchKey.isEmpty()) {
            return
        } else if (key.isEmpty()) {
            isLoading = true
            searchPage++
        } else if (key.isNotEmpty()) {
            isLoading = true
            searchPage = 1
            searchKey = key
            searchBooks.clear()
        }
        callBack?.startSearch()
        task = execute {
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
                    .onSuccess {
                        it?.let { list ->
                            searchSuccess(list)
                        }
                    }
            }
        }

        task?.invokeOnCompletion {
            callBack?.searchFinally()
            isLoading = false
        }
    }

    private suspend fun searchSuccess(searchBooks: List<SearchBook>) {
        withContext(IO) {
            val books = arrayListOf<SearchBook>()
            searchBooks.forEach { searchBook ->
                if (context.getPrefBoolean(PreferKey.precisionSearch)) {
                    if (searchBook.name.equals(searchKey, true)
                        || searchBook.author.equals(searchKey, true)
                    ) books.add(searchBook)
                } else
                    books.add(searchBook)
            }
            App.db.searchBookDao().insert(*books.toTypedArray())
            mergeItems(books)
        }
    }

    @Synchronized
    private fun mergeItems(newDataS: List<SearchBook>) {
        if (newDataS.isNotEmpty()) {
            val copyDataS = ArrayList(searchBooks)
            val searchBooksAdd = ArrayList<SearchBook>()
            if (copyDataS.size == 0) {
                copyDataS.addAll(newDataS)
            } else {
                //存在
                newDataS.forEach { item ->
                    var hasSame = false
                    for (searchBook in copyDataS) {
                        if (item.name == searchBook.name
                            && item.author == searchBook.author
                        ) {
                            hasSame = true
                            searchBook.addOrigin(item.bookUrl)
                            break
                        }
                    }
                    if (!hasSame) {
                        searchBooksAdd.add(item)
                    }
                }
                //添加
                searchBooksAdd.forEach { item ->
                    if (searchKey == item.name) {
                        for ((index, searchBook) in copyDataS.withIndex()) {
                            if (searchKey != searchBook.name) {
                                copyDataS.add(index, item)
                                break
                            }
                        }
                    } else if (searchKey == item.author) {
                        for ((i, searchBook) in copyDataS.withIndex()) {
                            if (searchKey != searchBook.name && searchKey == searchBook.author) {
                                copyDataS.add(i, item)
                                break
                            }
                        }
                    } else {
                        copyDataS.add(item)
                    }
                }
            }
            searchBooks = copyDataS
            searchBookLiveData.postValue(copyDataS)
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

    interface CallBack {
        fun startSearch()
        fun searchFinally()
    }
}
