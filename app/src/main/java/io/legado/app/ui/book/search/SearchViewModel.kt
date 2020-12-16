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
        val precision = context.getPrefBoolean(PreferKey.precisionSearch)
        App.db.searchBookDao.insert(*searchBooks.toTypedArray())
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
            val prevData = ArrayList(searchBooks)
            val precisionData = arrayListOf<SearchBook>()
            prevData.forEach {
                if (!scope.isActive) return
                if (it.name == searchKey || it.author == searchKey) {
                    precisionData.add(it)
                }
            }
            repeat(precisionData.size) {
                if (!scope.isActive) return
                prevData.removeAt(0)
            }
            newDataS.forEach { nBook ->
                if (!scope.isActive) return
                if (nBook.name == searchKey || nBook.author == searchKey) {
                    var hasSame = false
                    precisionData.forEach { pBook ->
                        if (!scope.isActive) return
                        if (pBook.name == nBook.name && pBook.author == nBook.author) {
                            pBook.addOrigin(nBook.origin)
                            hasSame = true
                        }
                    }
                    if (!hasSame) {
                        precisionData.add(nBook)
                    }
                } else if (!precision) {
                    var hasSame = false
                    prevData.forEach { pBook ->
                        if (!scope.isActive) return
                        if (pBook.name == nBook.name && pBook.author == nBook.author) {
                            pBook.addOrigin(nBook.origin)
                            hasSame = true
                        }
                    }
                    if (!hasSame) {
                        prevData.add(nBook)
                    }
                }
            }
            if (!scope.isActive) return
            precisionData.sortByDescending { it.origins.size }
            if (!scope.isActive) return
            if (!precision) {
                precisionData.addAll(prevData)
            }
            searchBooks = precisionData
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
            val searchBook = App.db.searchBookDao.getFirstByNameAuthor(name, author)
            success?.invoke(searchBook)
        }
    }

    /**
     * 保存搜索关键字
     */
    fun saveSearchKey(key: String) {
        execute {
            App.db.searchKeywordDao.get(key)?.let {
                it.usage = it.usage + 1
                App.db.searchKeywordDao.update(it)
            } ?: App.db.searchKeywordDao.insert(SearchKeyword(key, 1))
        }
    }

    /**
     * 清楚搜索关键字
     */
    fun clearHistory() {
        execute {
            App.db.searchKeywordDao.deleteAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchBookModel.close()
    }

}
