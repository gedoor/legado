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
import java.util.concurrent.Executors

class SearchViewModel(application: Application) : BaseViewModel(application) {
    private var searchPool =
        Executors.newFixedThreadPool(AppConfig.threadCount).asCoroutineDispatcher()
    private var task: Coroutine<*>? = null
    var isSearchLiveData = MutableLiveData<Boolean>()
    var searchBookLiveData = MutableLiveData<List<SearchBook>>()
    var searchKey: String = ""
    var searchPage = 1
    var isLoading = false
    var searchBooks = arrayListOf<SearchBook>()

    /**
     * 开始搜索
     */
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
        isSearchLiveData.postValue(true)
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
                    .onSuccess(IO) {
                        it?.let { list ->
                            if (context.getPrefBoolean(PreferKey.precisionSearch)) {
                                precisionSearch(list)
                            } else {
                                App.db.searchBookDao().insert(*list.toTypedArray())
                                mergeItems(list)
                            }
                        }
                    }
            }
        }

        task?.invokeOnCompletion {
            isSearchLiveData.postValue(false)
            isLoading = false
        }
    }

    /**
     * 精确搜索处理
     */
    private fun precisionSearch(searchBooks: List<SearchBook>) {
        val books = arrayListOf<SearchBook>()
        searchBooks.forEach { searchBook ->
            if (searchBook.name.contains(searchKey, true)
                || searchBook.author.contains(searchKey, true)
            ) books.add(searchBook)
        }
        App.db.searchBookDao().insert(*books.toTypedArray())
        mergeItems(books)
    }

    /**
     * 合并搜索结果并排序
     */
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
                        for ((index, searchBook) in copyDataS.withIndex()) {
                            if (searchKey != searchBook.name && searchKey == searchBook.author) {
                                copyDataS.add(index, item)
                                break
                            }
                        }
                    } else {
                        copyDataS.add(item)
                    }
                }
            }
            searchBooks.sortWith(Comparator { o1, o2 ->
                if (o1.name == searchKey && o2.name != searchKey) {
                    1
                } else if (o1.name != searchKey && o2.name == searchKey) {
                    -1
                } else if (o1.author == searchKey && o2.author != searchKey) {
                    1
                } else if (o1.author != searchKey && o2.author == searchKey) {
                    -1
                } else if (o1.name == o2.name) {
                    when {
                        o1.origins.size > o2.origins.size -> {
                            1
                        }
                        o1.origins.size < o2.origins.size -> {
                            -1
                        }
                        else -> {
                            0
                        }
                    }
                } else {
                    0
                }
            })
            searchBooks = copyDataS
            searchBookLiveData.postValue(copyDataS)
        }
    }

    /**
     * 停止搜索
     */
    fun stop() {
        task?.cancel()
    }

    /**
     * 按书名和作者获取书源排序最前的搜索结果
     */
    fun getSearchBook(name: String, author: String, success: ((searchBook: SearchBook?) -> Unit)?) {
        execute {
            val searchBook = App.db.searchBookDao().getFirstByNameAuthor(name, author)
            success?.invoke(searchBook)
        }
    }

    /**
     * 保存搜索关键字
     */
    fun saveSearchKey(key: String) {
        execute {
            App.db.searchKeywordDao().get(key)?.let {
                it.usage = it.usage + 1
                App.db.searchKeywordDao().update(it)
            } ?: App.db.searchKeywordDao().insert(SearchKeyword(key, 1))
        }
    }

    /**
     * 清楚搜索关键字
     */
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
