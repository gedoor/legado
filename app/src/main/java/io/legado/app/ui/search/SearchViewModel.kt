package io.legado.app.ui.search

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.SearchBook

class SearchViewModel(application: Application) : BaseViewModel(application) {

    val searchBooks: LiveData<List<SearchBook>> = MutableLiveData()

    fun search(start: (() -> Unit)? = null, finally: (() -> Unit)? = null) {
        execute {
            //            val response: String = HttpHelper.getApiService<CommonHttpApi>(
//                "http://www.baidu.com"
//            ).get("http://www.baidu.com").await()
//
//            delay(4000L)
//
//            Log.e("TAG1", Thread.currentThread().name)

            null
        }.timeout(30000L)
            .onStart {
                Log.e("TAG!", "start")
            }
            .onSuccess {
                Log.e("TAG!", "success: $it")
            }
            .onError {
                Log.e("TAG!", "error: $it")
            }
            .onFinally {
                Log.e("TAG!", "finally")
            }
    }

}
