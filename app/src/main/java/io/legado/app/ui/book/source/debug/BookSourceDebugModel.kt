package io.legado.app.ui.book.source.debug

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.model.Debug
import io.legado.app.model.webBook.WebBook

class BookSourceDebugModel(application: Application) : BaseViewModel(application),
    Debug.Callback {

    private var webBook: WebBook? = null
    private var callback: ((Int, String) -> Unit)? = null
    var searchSrc: String? = null
    var bookSrc: String? = null
    var tocSrc: String? = null
    var contentSrc: String? = null

    fun init(sourceUrl: String?) {
        sourceUrl?.let {
            //优先使用这个，不会抛出异常
            execute {
                val bookSource = appDb.bookSourceDao.getBookSource(sourceUrl)
                bookSource?.let { webBook = WebBook(it) }
            }
        }
    }

    fun observe(callback: (Int, String)-> Unit){
        this.callback = callback
    }

    fun startDebug(key: String, start: (() -> Unit)? = null, error: (() -> Unit)? = null) {
        execute {
            Debug.callback = this@BookSourceDebugModel
            Debug.startDebug(this, webBook!!, key)
        }.onStart {
            start?.invoke()
        }.onError {
            error?.invoke()
        }
    }

    override fun printLog(state: Int, msg: String) {
        when (state) {
            10 -> searchSrc = msg
            20 -> bookSrc = msg
            30 -> tocSrc = msg
            40 -> contentSrc = msg
            else -> callback?.invoke(state, msg)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Debug.cancelDebug(true)
    }

}
