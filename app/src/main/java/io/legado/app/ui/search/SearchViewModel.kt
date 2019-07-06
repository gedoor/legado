package io.legado.app.ui.search

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.api.CommonHttpApi
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.coroutine.Function
import io.legado.app.help.http.HttpHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import org.jetbrains.anko.error
import java.lang.StringBuilder

class SearchViewModel(application: Application) : BaseViewModel(application) {

    val searchBooks: LiveData<List<SearchBook>> = MutableLiveData()

    fun search(start: (() -> Unit)? = null, finally: (() -> Unit)? = null) {
//        launchOnUI(
//            {
//                start?.let { it() }
//                val searchResponse = withContext(IO) {
//                    val response: Deferred<String> = HttpHelper.getApiService<CommonHttpApi>(
//                        "http://www.baidu.com"
//                    ).get("http://www.baidu.com")
//
//                }
//
////                val result = searchResponse.await()
////
////                println(result)
//            },
//            { error { "${it.message}" } },
//            { finally?.let { it() } })

       val task =  Coroutine.of {
            val response: String = HttpHelper.getApiService<CommonHttpApi>(
                "http://www.baidu.com"
            ).get("http://www.baidu.com").await()

            Log.e("TAG1", Thread.currentThread().name)

            response
        }
            .onStart {
                Log.e("TAG!", "start")
            }
            .onSuccess {
               Log.e("TAG!", "success: $it")
            }
            .onError {
                error { "${it.message}" }
            }
            .onFinally {
                Log.e("TAG!", "finally")
            }

//        task.cancel()
    }

}
