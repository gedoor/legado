package io.legado.app.ui.book.source

import androidx.appcompat.app.AppCompatActivity
import io.legado.app.constant.AppLog
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.ui.login.SourceLoginJsExtensions
import io.legado.app.utils.isTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SourceCallBack {
    const val CLICK_AUTHOR = "clickAuthor"
    const val CLICK_BOOK_NAME = "clickBookName"
    const val CLICK_SHARE_BOOK = "clickShareBook"
    const val CLICK_CUSTOM_BUTTON = "clickCustomButton"
    const val ADD_BOOK_SHELF = "addBookShelf"
    const val DEL_BOOK_SHELF = "delBookShelf"
    const val CLEAR_CACHE = "clearCache"
    const val SAVE_READ = "saveRead"
    const val START_READ = "startRead"
    const val END_READ = "endRead"
    const val START_SHELF_REFRESH = "startShelfRefresh"
    const val END_SHELF_REFRESH = "endShelfRefresh"
    fun callBackBtn(activity: AppCompatActivity, event: String, source: BookSource?, book: Book, chapter: BookChapter?, noCall: (() -> Unit)? = null) {
        if (source == null || !source.eventListener) {
            noCall?.invoke()
            return
        }
        val jsStr = source.getContentRule().callBackJs
        if (jsStr.isNullOrEmpty()) {
            noCall?.invoke()
            return
        }
        Coroutine.async {
            val java = SourceLoginJsExtensions(activity, source)
            val result = source.evalJS(jsStr) {
                put("event", event)
                put("java", java)
                put("result", null)
                put("book", book)
                put("chapter", chapter)
            }.toString()
            if (!result.isTrue()) {
                withContext(Dispatchers.Main) {
                    noCall?.invoke()
                }
            }
        }.onError {
            AppLog.put("书源执行回调事件${event}出错\n${it.localizedMessage}", it, true)
        }
    }

    fun callBackBook(event: String, source: BookSource?, book: Book?) {
        if (source == null || book == null || !source.eventListener) return
        val jsStr = source.getContentRule().callBackJs
        if (jsStr.isNullOrEmpty()) return
        Coroutine.async {
            source.evalJS(jsStr) {
                put("event", event)
                put("result", null)
                put("book", book)
                put("chapter", null)
            }
        }.onError {
            AppLog.put("书源执行回调事件${event}出错\n${it.localizedMessage}", it, true)
        }
    }

    fun callBackSource(event: String, source: BookSource) {
        val jsStr = source.getContentRule().callBackJs
        if (jsStr.isNullOrEmpty()) return
        Coroutine.async {
            source.evalJS(jsStr) {
                put("event", event)
                put("result", null)
                put("book", null)
                put("chapter", null)
            }
        }.onError {
            AppLog.put("书源执行回调事件${event}出错\n${it.localizedMessage}", it, true)
        }
    }

}