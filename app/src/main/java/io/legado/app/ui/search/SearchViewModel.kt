package io.legado.app.ui.search

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.api.CommonHttpApi
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.coroutine.CompositeCoroutine
import io.legado.app.help.http.HttpHelper
import io.legado.app.model.WebBook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

class SearchViewModel(application: Application) : BaseViewModel(application) {
    val tasks: CompositeCoroutine = CompositeCoroutine()
    val searchBookList = arrayListOf<SearchBook>()
    val searchBooksData: LiveData<List<SearchBook>> = MutableLiveData()
    var searchPage = 0
    private val channel = Channel<Int>()//协程之间通信

    fun search(key: String, start: (() -> Unit)? = null, finally: (() -> Unit)? = null) {
        if (key.isEmpty()) return
        start?.invoke()
        execute {
            val bookSourceList = App.db.bookSourceDao().allEnabled
            for (item in bookSourceList) {
                val search = WebBook(item).searchBook(key, searchPage)
                    .onSuccess { searchBookS ->
                        searchBookS?.let { searchBookList.addAll(it) }
                    }
                tasks.add(search)
            }

        }
    }

    suspend fun test(scope: CoroutineScope): MutableList<String> {
        val list = mutableListOf<String>()
        repeat(10) {
            withContext(scope.coroutineContext) {
                Log.e("TAG3", Thread.currentThread().name)
                val response: String = HttpHelper.getApiService<CommonHttpApi>(
                    "http://www.baidu.com"
                ).get("http://www.baidu.com").await()
                list.add(response)
            }
        }
        return list
    }

}
