package io.legado.app.ui.book.search

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.SearchKeyword
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.WebBook
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class SearchViewModel(application: Application) : BaseViewModel(application) {
    private var searchPool = Executors.newFixedThreadPool(16).asCoroutineDispatcher()
    private var task: Coroutine<*>? = null
    var callBack: CallBack? = null
    var searchKey: String = ""
    var startTime: Long = 0
    var searchPage = 0
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
            searchPage = 0
            searchKey = key
            searchBooks.clear()
        }
        startTime = System.currentTimeMillis()
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
                    .onSuccess(Dispatchers.IO) {
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

    private fun searchSuccess(searchBooks: List<SearchBook>) {
        val books = arrayListOf<SearchBook>()
        searchBooks.forEach { searchBook ->
            if (context.getPrefBoolean(PreferKey.precisionSearch)) {
                if (searchBook.name.contains(searchKey)
                    || searchBook.author.contains(searchKey)
                ) books.add(searchBook)
            } else
                books.add(searchBook)
        }
        App.db.searchBookDao().insert(*books.toTypedArray())
        addToAdapter(books)
    }

    @Synchronized
    private fun addToAdapter(newDataS: List<SearchBook>) {
        if (newDataS.isNotEmpty()) {
            val copyDataS = ArrayList(searchBooks)
            val searchBooksAdd = ArrayList<SearchBook>()
            if (copyDataS.size == 0) {
                copyDataS.addAll(newDataS)
            } else {
                //存在
                for (temp in newDataS) {
                    var hasSame = false
                    var i = 0
                    val size = copyDataS.size
                    while (i < size) {
                        val searchBook = copyDataS[i]
                        if (temp.name == searchBook.name
                            && temp.author == searchBook.author
                        ) {
                            hasSame = true
                            searchBook.addOrigin(temp.bookUrl)
                            break
                        }
                        i++
                    }
                    if (!hasSame) {
                        searchBooksAdd.add(temp)
                    }
                }
                //添加
                for (temp in searchBooksAdd) {
                    if (searchKey == temp.name) {
                        for (i in copyDataS.indices) {
                            val searchBook = copyDataS[i]
                            if (searchKey != searchBook.name) {
                                copyDataS.add(i, temp)
                                break
                            }
                        }
                    } else if (searchKey == temp.author) {
                        for (i in copyDataS.indices) {
                            val searchBook = copyDataS[i]
                            if (searchKey != searchBook.name && searchKey == searchBook.author) {
                                copyDataS.add(i, temp)
                                break
                            }
                        }
                    } else {
                        copyDataS.add(temp)
                    }
                }
            }
            launch {
                searchBooks = copyDataS
                callBack?.adapter?.setItems(searchBooks)
            }
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
        var adapter: SearchAdapter
        fun startSearch()
        fun searchFinally()
    }
}
