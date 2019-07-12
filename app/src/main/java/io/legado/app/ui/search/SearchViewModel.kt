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
import kotlinx.coroutines.launch


class SearchViewModel(application: Application) : BaseViewModel(application) {

    val searchBooks: LiveData<List<SearchBook>> = MutableLiveData()

    fun search(start: (() -> Unit)? = null, finally: (() -> Unit)? = null) {
        launch {
            delay(1000L)


            repeat(100) {
                test(it)
            }

        }
    }


    private fun test(index: Int) {
        submit {
            val response = HttpHelper.getApiService<CommonHttpApi>(
                "http://www.baidu.com"
            ).get("http://www.baidu.com")

            Log.e("TAG", "next: $index")

            response
        }
            .onStart {
                Log.e("TAG!", "start: $index")
            }
            .onSuccess {
                Log.e("TAG!", "success: $index --> $it")
            }
            .onError {
                Log.e("TAG!", "error: $index --> $it")
            }
            .onFinally {
                Log.e("TAG!", "finally")
            }
    }

}
