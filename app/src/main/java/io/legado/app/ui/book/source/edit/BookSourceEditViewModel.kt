package io.legado.app.ui.book.source.edit

import android.app.Application
import android.content.Intent
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.help.storage.OldRule
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.getClipText
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers

class BookSourceEditViewModel(application: Application) : BaseViewModel(application) {

    var bookSource: BookSource? = null
    private var oldSourceUrl: String? = null

    fun initData(intent: Intent, onFinally: () -> Unit) {
        execute {
            val key = intent.getStringExtra("data")
            var source: BookSource? = null
            if (key != null) {
                source = appDb.bookSourceDao.getBookSource(key)
            }
            source?.let {
                oldSourceUrl = it.bookSourceUrl
                bookSource = it
            }
        }.onFinally {
            onFinally()
        }
    }

    fun save(source: BookSource, success: (() -> Unit)? = null) {
        execute {
            oldSourceUrl?.let {
                if (oldSourceUrl != source.bookSourceUrl) {
                    appDb.bookSourceDao.delete(it)
                }
            }
            oldSourceUrl = source.bookSourceUrl
            source.lastUpdateTime = System.currentTimeMillis()
            appDb.bookSourceDao.insert(source)
            bookSource = source
        }.onSuccess {
            success?.invoke()
        }.onError {
            context.toastOnUi(it.localizedMessage)
            it.printStackTrace()
        }
    }

    fun pasteSource(onSuccess: (source: BookSource) -> Unit) {
        execute(context = Dispatchers.Main) {
            var source: BookSource? = null
            context.getClipText()?.let { json ->
                source = OldRule.jsonToBookSource(json)
            }
            source
        }.onError {
            context.toastOnUi(it.localizedMessage)
            it.printStackTrace()
        }.onSuccess {
            if (it != null) {
                onSuccess(it)
            } else {
                context.toastOnUi("格式不对")
            }
        }
    }

    fun importSource(text: String, finally: (source: BookSource) -> Unit) {
        execute {
            val text1 = text.trim()
            GSON.fromJsonObject<BookSource>(text1)?.let {
                finally.invoke(it)
            }
        }.onError {
            context.toastOnUi(it.localizedMessage ?: "Error")
        }
    }
}