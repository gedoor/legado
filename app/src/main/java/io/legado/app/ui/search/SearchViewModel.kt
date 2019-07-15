package io.legado.app.ui.search

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.api.CommonHttpApi
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.http.HttpHelper
import io.legado.app.model.WebBook
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : BaseViewModel(application) {

    val searchBooks: LiveData<List<SearchBook>> = MutableLiveData()

    fun search(start: (() -> Unit)? = null, finally: (() -> Unit)? = null) {
        val c = execute {
            val response: String = HttpHelper.getApiService<CommonHttpApi>(
                "http://www.baidu.com"
            ).get("http://www.baidu.com").await()

            delay(2000L)

            response

        }
            .onStart {
                Log.e("TAG!", "start")
                start?.let { it() }
            }
            .onSuccess {
                Log.e("TAG!", "success: $it")
            }
            .onError {
                Log.e("TAG!", "error: $it")
            }
            .onFinally {
                Log.e("TAG!", "finally")
                if (finally != null) {
                    finally()
                }
            }

        val c2 = plus(c)
//            .timeout { 100L }
//            .onErrorReturn { "error return2" }
            .onStart {
                Log.e("TAG!", "start2")
                start?.let { it() }
            }
            .onSuccess {
                Log.e("TAG!", "success2: $it")
            }
            .onError {
                Log.e("TAG!", "error2: $it")
            }
            .onFinally {
                Log.e("TAG!", "finally2")
                if (finally != null) {
                    finally()
                }
            }

        launch {

           delay(1500L)
            c2.cancel()

        }
    }

}
