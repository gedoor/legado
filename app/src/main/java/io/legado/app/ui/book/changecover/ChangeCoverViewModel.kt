package io.legado.app.ui.book.changecover

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.help.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.WebBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class ChangeCoverViewModel(application: Application) : BaseViewModel(application) {
    private var searchPool =
        Executors.newFixedThreadPool(AppConfig.threadCount).asCoroutineDispatcher()
    var callBack: CallBack? = null
    var name: String = ""
    var author: String = ""
    private var task: Coroutine<*>? = null
    val searchStateData = MutableLiveData<Boolean>()

    fun initData() {
        execute {
            App.db.searchBookDao().getEnableHasCover(name, author)
        }.onSuccess {
            it?.let {
                callBack?.adapter?.setItems(it)
            }
        }.onFinally {
            search()
        }
    }

    fun search() {
        task = execute {
            searchStateData.postValue(true)
            val bookSourceList = App.db.bookSourceDao().allEnabled
            for (item in bookSourceList) {
                //task取消时自动取消 by （scope = this@execute）
                WebBook(item).searchBook(name, scope = this@execute, context = searchPool)
                    .timeout(30000L)
                    .onSuccess(Dispatchers.IO) {
                        if (it != null && it.isNotEmpty()) {
                            val searchBook = it[0]
                            if (searchBook.name == name && searchBook.author == author
                                && !searchBook.coverUrl.isNullOrEmpty()
                            ) {
                                App.db.searchBookDao().insert(searchBook)
                                callBack?.adapter?.let { adapter ->
                                    if (!adapter.getItems().contains(searchBook)) {
                                        withContext(Dispatchers.Main) {
                                            adapter.addItem(searchBook)
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
        }

        task?.invokeOnCompletion {
            searchStateData.postValue(false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchPool.close()
    }

    interface CallBack {
        var adapter: CoverAdapter
    }
}