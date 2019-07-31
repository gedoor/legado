package io.legado.app.ui.sourcedebug

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.EventMessage
import io.legado.app.model.WebBook
import io.legado.app.model.webbook.SourceDebug
import io.legado.app.utils.isAbsUrl

class SourceDebugModel(application: Application) : BaseViewModel(application), SourceDebug.Callback {

    val logs: MutableLiveData<EventMessage> = MutableLiveData()

    private var bookSource: BookSource? = null

    fun init(sourceUrl: String?) {
        sourceUrl?.let {
            //优先使用这个，不会抛出异常
            execute {
                bookSource = App.db.bookSourceDao().findByKey(sourceUrl)
            }
        }
    }

    fun startDebug(key: String, start: (() -> Unit)? = null, error: (() -> Unit)? = null) {
        bookSource?.let {
            start?.let { it() }
            SourceDebug.debugSource = it.bookSourceUrl
            if (key.isAbsUrl()) {
                val book = Book()
                book.origin = it.bookSourceUrl
                book.bookUrl = key
                SourceDebug.printLog(it.bookSourceUrl, 1, "开始访问$key")
                SourceDebug(WebBook(it), this)
                    .infoDebug(book)
            } else {
                SourceDebug.printLog(it.bookSourceUrl, 1, "开始搜索关键字$key")
                SourceDebug(WebBook(it), this)
                    .searchDebug(key)
            }
        } ?: error?.let { it() }
    }

    override fun printLog(state: Int, msg: String) {
        logs.postValue(EventMessage.obtain(state, msg))
    }
}
