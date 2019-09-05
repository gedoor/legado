package io.legado.app.ui.sourceedit

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.BookSource
import io.legado.app.help.storage.OldRule

class SourceEditViewModel(application: Application) : BaseViewModel(application) {

    val sourceLiveData: MutableLiveData<BookSource> = MutableLiveData()

    fun setBookSource(key: String) {
        execute {
            App.db.bookSourceDao().getBookSource(key)?.let {
                sourceLiveData.postValue(it)
            } ?: sourceLiveData.postValue(BookSource())
        }
    }

    fun save(bookSource: BookSource, finally: (() -> Unit)? = null) {
        execute {
            if (bookSource.customOrder == 0) {
                bookSource.customOrder = App.db.bookSourceDao().allCount()
            }
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