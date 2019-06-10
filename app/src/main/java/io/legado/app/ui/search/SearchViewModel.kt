package io.legado.app.ui.search

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.api.CommonHttpApi
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.http.HttpHelper
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.jetbrains.anko.error

class SearchViewModel(application: Application) : BaseViewModel(application) {

    val searchBooks: LiveData<List<SearchBook>> = MutableLiveData()

    fun search(start: () -> Unit, finally: () -> Unit) {
        launchOnUI(
            {
                start()
                val searchResponse = withContext(IO) {
                    HttpHelper.getApiService<CommonHttpApi>(
                        "http:www.baidu.com"
                    ).get("", mutableMapOf())
                }

                val result = searchResponse.await()
            },
            { error { "${it.message}" } },
            { finally() })
    }

}
