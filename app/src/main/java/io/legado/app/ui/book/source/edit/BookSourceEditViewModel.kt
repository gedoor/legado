package io.legado.app.ui.book.source.edit

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.BookSource
import io.legado.app.help.storage.OldRule

class BookSourceEditViewModel(application: Application) : BaseViewModel(application) {

    var bookSource: BookSource? = null
    private var oldSourceUrl: String? = null

    fun initData(intent: Intent, onFinally: () -> Unit) {
        execute {
            val key = intent.getStringExtra("data")
            var source: BookSource? = null
            if (key != null) {
                source = App.db.bookSourceDao().getBookSource(key)
            }
            source?.let {
                oldSourceUrl = it.bookSourceUrl
                bookSource = it
            } ?: let {
                bookSource = BookSource().apply {
                    customOrder = App.db.bookSourceDao().maxOrder + 1
                }
            }
        }.onFinally {
            onFinally()
        }
    }

    fun save(bookSource: BookSource, success: (() -> Unit)? = null) {
        execute {
            oldSourceUrl?.let {
                if (oldSourceUrl != bookSource.bookSourceUrl) {
                    App.db.bookSourceDao().delete(it)
                }
            }
            oldSourceUrl = bookSource.bookSourceUrl
            App.db.bookSourceDao().insert(bookSource)
        }.onSuccess {
            success?.invoke()
        }.onError {
            toast(it.localizedMessage)
            it.printStackTrace()
        }
    }

    fun pasteSource(onSuccess: () -> Unit) {
        execute {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            clipboard?.primaryClip?.let {
                if (it.itemCount > 0) {
                    val json = it.getItemAt(0).text.toString()
                    OldRule.jsonToBookSource(json)?.let { source ->
                        bookSource = source
                    } ?: toast("格式不对")
                }
            }
        }.onError {
            toast(it.localizedMessage)
        }.onSuccess {
            onSuccess()
        }
    }
}