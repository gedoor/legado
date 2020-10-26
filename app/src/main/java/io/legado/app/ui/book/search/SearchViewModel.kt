package io.legado.app.ui.book.search

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.SearchKeyword
import io.legado.app.model.webBook.SearchBookModel
import io.legado.app.utils.getPrefBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive

class SearchViewModel(application: Application) : BaseViewModel(application),
    SearchBookModel.CallBack {
    val handler = Handler(Looper.getMainLooper())
    private val searchBookModel = SearchBookModel(this, this)
    var isSearchLiveData = MutableLiveData<Boolean>()
    var searchBookLiveData = MutableLiveData<List<SearchBook>>()
    var searchKey: String = ""
    var isLoading = false
    private var searchBooks = arrayListOf<SearchBook>()
    private var searchID = 0L
    private var postTime = 0L
    private val sendRunnable = Runnable { upAdapter() }

    /**
     * 开始搜索
     */
    fun search(key: String) {
        if ((searchKey == key) || key.isNotEmpty()) {
            searchBookModel.cancelSearch()
            searchBooks.clear()
            searchBookLiveData.postValue(searchBooks)
            searchID = System.currentTimeMillis()
            searchKey = key
        }
        if (searchKey.isEmpty()) {
            return
        }
        searchBookModel.search(searchID, searchKey)
    }

    @Synchronized
    private fun upAdapter() {
        if (System.currentTimeMillis() >= postTime + 500) {
            handler.removeCallbacks(sendRunnable)
            postTime = System.currentTimeMillis()
            searchBookLiveData.postValue(searchBooks)
        } else {
            handler.removeCallbacks(sendRunnable)
            handler.postDelayed(sendRunnable, 500 - System.currentTimeMillis() + postTime)
        }
    }

    override fun onSearchStart() {
        isSearchLiveData.postValue(true)
        isLoading = true
    }

    override fun onSearchSuccess(searchBooks: ArrayList<SearchBook>) {
        if (context.getPrefBoolean(PreferKey.precisionSearch)) {
            precisionSearch(this, searchBooks)
        } else {
            App.db.searchBookDao().insert(*searchBooks.toTypedArray())
            mergeItems(this, searchBooks)
        }
    }

    override fun onSearchFinish() {
        isSearchLiveData.postValue(false)
        isLoading = false
    }

    override fun onSearchCancel() {
        isSearchLiveData.postValue(false)
        isLoading = false
    }

    /**
     * 精确搜索处理
     */
    private fun precisionSearch(scope: CoroutineScope, searchBooks: List<SearchBook>) {
        val books = arrayListOf<SearchBook>()
        searchBooks.forEach { searchBook ->
            if (searchBook.name.contains(searchKey, true)
                || searchBook.author.contains(searchKey, true)
            ) books.add(searchBook)
        }
        App.db.searchBookDao().insert(*books.toTypedArray())
        if (scope.isActive) {
            mergeItems(scope, books)
        }
    }

    /**
     * 合并搜索结果并排序
     */
    @Synchronized
    private fun mergeItems(scope: CoroutineScope, newDataS: List<SearchBook>) {
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
                            searchBook.addOrigin(item.origin)
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
            if (!scope.isActive) return
            searchBooks.sortWith { o1, o2 ->
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
            }
            if (!scope.isActive) return
            searchBooks = copyDataS
            upAdapter()
        }
    }

    /**
     * 停止搜索
     */
    fun stop() {
        searchBookModel.cancelSearch()
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
        searchBookModel.close()
    }

}
