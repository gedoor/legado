package io.legado.app.ui.book.search

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
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
        val precision = context.getPrefBoolean(PreferKey.precisionSearch)
        appDb.searchBookDao.insert(*searchBooks.toTypedArray())
        mergeItems(this, searchBooks, precision)
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
     * 合并搜索结果并排序
     */
    @Synchronized
    private fun mergeItems(scope: CoroutineScope, newDataS: List<SearchBook>, precision: Boolean) {
        if (newDataS.isNotEmpty()) {
            val copyData = ArrayList(searchBooks)
            val equalData = arrayListOf<SearchBook>()
            val containsData = arrayListOf<SearchBook>()
            val otherData = arrayListOf<SearchBook>()
            copyData.forEach {
                if (!scope.isActive) return
                if (it.name == searchKey || it.author == searchKey) {
                    equalData.add(it)
                } else if (it.name.contains(searchKey) || it.author.contains(searchKey)) {
                    containsData.add(it)
                } else {
                    otherData.add(it)
                }
            }
            newDataS.forEach { nBook ->
                if (!scope.isActive) return
                if (nBook.name == searchKey || nBook.author == searchKey) {
                    var hasSame = false
                    equalData.forEach { pBook ->
                        if (!scope.isActive) return
                        if (pBook.name == nBook.name && pBook.author == nBook.author) {
                            pBook.addOrigin(nBook.origin)
                            hasSame = true
                        }
                    }
                    if (!hasSame) {
                        equalData.add(nBook)
                    }
                } else if (nBook.name.contains(searchKey) || nBook.author.contains(searchKey)) {
                    var hasSame = false
                    containsData.forEach { pBook ->
                        if (!scope.isActive) return
                        if (pBook.name == nBook.name && pBook.author == nBook.author) {
                            pBook.addOrigin(nBook.origin)
                            hasSame = true
                        }
                    }
                    if (!hasSame) {
                        containsData.add(nBook)
                    }
                } else if (!precision) {
                    var hasSame = false
                    otherData.forEach { pBook ->
                        if (!scope.isActive) return
                        if (pBook.name == nBook.name && pBook.author == nBook.author) {
                            pBook.addOrigin(nBook.origin)
                            hasSame = true
                        }
                    }
                    if (!hasSame) {
                        otherData.add(nBook)
                    }
                }
            }
            if (!scope.isActive) return
            equalData.sortByDescending { it.origins.size }
            equalData.addAll(containsData.sortedByDescending { it.origins.size })
            if (!precision) {
                equalData.addAll(otherData)
            }
            searchBooks = equalData
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
            val searchBook = appDb.searchBookDao.getFirstByNameAuthor(name, author)
            success?.invoke(searchBook)
        }
    }

    /**
     * 保存搜索关键字
     */
    fun saveSearchKey(key: String) {
        execute {
            appDb.searchKeywordDao.get(key)?.let {
                it.usage = it.usage + 1
                appDb.searchKeywordDao.update(it)
            } ?: appDb.searchKeywordDao.insert(SearchKeyword(key, 1))
        }
    }

    /**
     * 清楚搜索关键字
     */
    fun clearHistory() {
        execute {
            appDb.searchKeywordDao.deleteAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchBookModel.close()
    }

}
