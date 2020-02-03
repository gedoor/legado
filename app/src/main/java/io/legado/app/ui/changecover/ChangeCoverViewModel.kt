package io.legado.app.ui.changecover

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.help.AppConfig
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class ChangeCoverViewModel(application: Application) : BaseViewModel(application) {
    private var searchPool =
        Executors.newFixedThreadPool(AppConfig.threadCount).asCoroutineDispatcher()
    var callBack: CallBack? = null
    var name: String = ""
    var author: String = ""

    fun initData() {
        execute {
            App.db.searchBookDao().getEnableHasCover(name, author).let {
                callBack?.adapter?.setItems(it)
            }
        }
    }

    fun search() {

    }

    override fun onCleared() {
        super.onCleared()
        searchPool.close()
    }

    interface CallBack {
        var adapter: CoverAdapter
    }
}