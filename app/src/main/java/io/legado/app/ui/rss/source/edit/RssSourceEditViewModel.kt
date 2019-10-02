package io.legado.app.ui.rss.source.edit

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssSource
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject

class RssSourceEditViewModel(application: Application) : BaseViewModel(application) {

    val sourceLiveData: MutableLiveData<RssSource> = MutableLiveData()

    fun setSource(key: String) {
        execute {
            App.db.rssSourceDao().getByKey(key)?.let {
                sourceLiveData.postValue(it)
            } ?: sourceLiveData.postValue(RssSource())
        }
    }

    fun save(rssSource: RssSource, success: (() -> Unit)) {
        execute {
            App.db.rssSourceDao().insert(rssSource)
        }.onSuccess {
            success()
        }
    }

    fun pasteSource() {
        execute {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            clipboard?.primaryClip?.let {
                if (it.itemCount > 0) {
                    val json = it.getItemAt(0).text.toString()
                    GSON.fromJsonObject<RssSource>(json)?.let { source ->
                        sourceLiveData.postValue(source)
                    } ?: toast("格式不对")
                }
            }
        }
    }
}