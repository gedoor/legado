package io.legado.app.ui.book.search

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.SearchKeyword
import io.legado.app.model.webBook.SearchModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : BaseViewModel(application), SearchModel.CallBack {
    private val searchModel = SearchModel(viewModelScope, this)
    private var upAdapterJob: Job? = null
    var isSearchLiveData = MutableLiveData<Boolean>()
    var searchBookLiveData = MutableLiveData<List<SearchBook>>()
    var searchKey: String = ""
    var isLoading = false
    private var searchBooks = arrayListOf<SearchBook>()
    private var searchID = 0L
    private var postTime = 0L

    /**
     * 开始搜索
     */
    fun search(key: String) {
        if ((searchKey == key) || key.isNotEmpty()) {
            searchModel.cancelSearch()
            searchBooks.clear()
            searchBookLiveData.postValue(searchBooks)
            searchID = System.currentTimeMillis()
            searchKey = key
        }
        if (searchKey.isEmpty()) {
            return
        }
        searchModel.search(searchID, searchKey)
    }

    @Synchronized
    private fun upAdapter() {
        upAdapterJob?.cancel()
        if (System.currentTimeMillis() >= postTime + 1000) {
            postTime = System.currentTimeMillis()
            searchBookLiveData.postValue(searchBooks)
        } else {
            upAdapterJob = viewModelScope.launch {
                delay(1000)
                upAdapter()
            }
        }
    }

    override fun onSearchStart() {
        isSearchLiveData.postValue(true)
        isLoading = true
    }

    override fun onSearchSuccess(searchBooks: ArrayList<SearchBook>) {
        this.searchBooks = searchBooks
        upAdapter()
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
     * 停止搜索
     */
    fun stop() {
        searchModel.cancelSearch()
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

    fun deleteHistory(searchKeyword: SearchKeyword) {
        appDb.searchKeywordDao.delete(searchKeyword)
    }

    override fun onCleared() {
        super.onCleared()
        searchModel.close()
    }

}
