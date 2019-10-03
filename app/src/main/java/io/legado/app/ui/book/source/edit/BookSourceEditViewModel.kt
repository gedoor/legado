package io.legado.app.ui.book.source.edit

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.BookSource
import io.legado.app.help.storage.OldRule

class BookSourceEditViewModel(application: Application) : BaseViewModel(application) {

    val sourceLiveData: MutableLiveData<BookSource> = MutableLiveData()
    var oldSourceUrl: String? = null

    fun setBookSource(key: String?) {
        execute {
            var source: BookSource? = null
            if (key != null) {
                source = App.db.bookSourceDao().getBookSource(key)
            }
            source?.let {
                oldSourceUrl = it.bookSourceUrl
                sourceLiveData.postValue(it)
            } ?: let {
                sourceLiveData.postValue(BookSource().apply {
                    customOrder = App.db.bookSourceDao().maxOrder + 1
                })
            }
        }
    }

    fun save(bookSource: BookSource, finally: (() -> Unit)? = null) {
        execute {
            oldSourceUrl?.let {
                if (oldSourceUrl != bookSource.bookSourceUrl) {
                    App.db.bookSourceDao().delete(it)
                }
            }
            oldSourceUrl = bookSource.bookSourceUrl
            App.db.bookSourceDao().insert(bookSource)
        }.onFinally {
            finally?.let { it() }
        } 
    }

    fun pasteSource() {
        execute {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            clipboard?.primaryClip?.let {
                if (it.itemCount > 0) {
                    val json = it.getItemAt(0).text.toString()
                    OldRule.jsonToBookSource(json)?.let { source ->
                        sourceLiveData.postValue(source)
                    } ?: toast("格式不对")
                }
            }
        }
    }
}