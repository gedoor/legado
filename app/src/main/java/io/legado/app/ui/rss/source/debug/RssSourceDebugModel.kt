package io.legado.app.ui.rss.source.debug

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssSource
import io.legado.app.model.Debug

class RssSourceDebugModel(application: Application) : BaseViewModel(application),
    Debug.Callback {

    private var rssSource: RssSource? = null

    private var callback: ((Int, String) -> Unit)? = null

    fun initData(sourceUrl: String?, finally: () -> Unit) {
        sourceUrl?.let {
            execute {
                rssSource = App.db.rssSourceDao.getByKey(sourceUrl)
            }.onFinally {
                finally()
            }
        }
    }

    fun observe(callback: (Int, String) -> Unit) {
        this.callback = callback
    }

    fun startDebug(start: (() -> Unit)? = null, error: (() -> Unit)? = null) {
        rssSource?.let {
            start?.invoke()
            Debug.callback = this
            Debug.startDebug(it)
        } ?: error?.invoke()
    }

    override fun printLog(state: Int, msg: String) {
        callback?.invoke(state, msg)
    }

    override fun onCleared() {
        super.onCleared()
        Debug.cancelDebug(true)
    }

}
