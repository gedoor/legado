package io.legado.app.ui.book.source

import io.legado.app.constant.AppLog
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.login.SourceLoginJsExtensions
import io.legado.app.utils.isTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SourceCallBack {
    fun callBackShare(activity: BookInfoActivity, source: BookSource?, book: Book, noCall: () -> Unit) {
        if (source == null || !source.eventListener) {
            noCall.invoke()
            return
        }
        val jsStr = source.getContentRule().callBackJs
        if (jsStr.isNullOrEmpty()) {
            noCall.invoke()
            return
        }
        Coroutine.async {
            val java = SourceLoginJsExtensions(activity, source)
            val result = source.evalJS(jsStr) {
                put("event", "shareBook")
                put("java", java)
                put("result", null)
                put("book", book)
                put("chapter", null)
            }.toString()
            if (!result.isTrue()) {
                withContext(Dispatchers.Main) {
                    noCall.invoke()
                }
            }
        }.onError {
            AppLog.put("书源执行分享回调js函数出错\n${it.localizedMessage}", it, true)
        }
    }

    fun callBackBookShelf(source: BookSource?, book: Book?, isAdd: Boolean) {
        if (source == null || book == null || !source.eventListener) return
        val jsStr = source.getContentRule().callBackJs
        if (jsStr.isNullOrEmpty()) return
        Coroutine.async {
            val event = if (isAdd) "addBookShelf" else "delBookShelf"
            source.evalJS(jsStr) {
                put("event", event)
                put("result", null)
                put("book", book)
                put("chapter", null)
            }
        }.onError {
            AppLog.put("书源执行书架事件回调js函数出错\n${it.localizedMessage}", it, true)
        }
    }

    fun callBackClearCache(source: BookSource?, book: Book) {
        if (source == null || !source.eventListener) return
        val jsStr = source.getContentRule().callBackJs
        if (jsStr.isNullOrEmpty()) return
        Coroutine.async {
            source.evalJS(jsStr) {
                put("event", "clearCache")
                put("result", null)
                put("book", book)
                put("chapter", null)
            }
        }.onError {
            AppLog.put("书源执行清除缓存事件回调js函数出错\n${it.localizedMessage}", it, true)
        }
    }

    fun callBackSaveRead(source: BookSource?, book: Book) {
        if (source == null || !source.eventListener) return
        val jsStr = source.getContentRule().callBackJs
        if (jsStr.isNullOrEmpty()) return
        Coroutine.async {
            source.evalJS(jsStr) {
                put("event", "saveRead")
                put("result", null)
                put("book", book)
                put("chapter", null)
            }
        }.onError {
            AppLog.put("书源执行保存阅读进度事件回调js函数出错\n${it.localizedMessage}", it, true)
        }
    }

}