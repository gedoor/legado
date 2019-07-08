package io.legado.app.ui.search

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.api.CommonHttpApi
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.http.HttpHelper
import kotlinx.coroutines.delay
import org.jetbrains.anko.error

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

        execute {
            val response: String = HttpHelper.getApiService<CommonHttpApi>(
                "http://www.baidu.com"
            ).get("http://www.baidu.com").await()

            delay(4000L)

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

    }

}
