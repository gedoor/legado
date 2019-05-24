package io.legado.app.ui.search

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.api.CommonHttpApi
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.http.HttpHelper
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class SearchViewModel(application: Application) : BaseViewModel(application) {

    val searchBooks: LiveData<List<SearchBook>> = MutableLiveData()

    public fun search(start: () -> Unit, finally: () -> Unit) {
        launchOnUI(
            {
                start()
                val searchResponse = withContext(IO) {
                    HttpHelper.getApiService(
                        "http:www.baidu.com",
                        CommonHttpApi::class.java
                    ).get("", mutableMapOf())
                }

                val result = searchResponse.await()
            },
            { Log.i("TAG", "${it.message}") },
            { finally() })

//        GlobalScope.launch {
//
//        }
    }

}
