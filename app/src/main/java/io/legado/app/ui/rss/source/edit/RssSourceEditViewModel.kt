package io.legado.app.ui.rss.source.edit

import android.app.Application
import android.content.Intent
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssSource
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.getClipText
import io.legado.app.utils.msg
import kotlinx.coroutines.Dispatchers

class RssSourceEditViewModel(application: Application) : BaseViewModel(application) {

    var rssSource: RssSource = RssSource()
    private var oldSourceUrl: String = ""

    fun initData(intent: Intent, onFinally: () -> Unit) {
        execute {
            val key = intent.getStringExtra("data")
            if (key != null) {
                App.db.rssSourceDao.getByKey(key)?.let {
                    rssSource = it
                }
            }
            oldSourceUrl = rssSource.sourceUrl
        }.onFinally {
            onFinally()
        }
    }

    fun save(source: RssSource, success: (() -> Unit)) {
        execute {
            if (oldSourceUrl != source.sourceUrl) {
                App.db.rssSourceDao.delete(oldSourceUrl)
                oldSourceUrl = source.sourceUrl
            }
            App.db.rssSourceDao.insert(source)
        }.onSuccess {
            success()
        }.onError {
            toast(it.localizedMessage)
            it.printStackTrace()
        }
    }

    fun pasteSource(onSuccess: (source: RssSource) -> Unit) {
        execute(context = Dispatchers.Main) {
            var source: RssSource? = null
            context.getClipText()?.let { json ->
                source = GSON.fromJsonObject<RssSource>(json)
            }
            source
        }.onError {
            toast(it.localizedMessage)
        }.onSuccess {
            if (it != null) {
                onSuccess(it)
            } else {
                toast("格式不对")
            }
        }
    }

    fun importSource(text: String, finally: (source: RssSource) -> Unit) {
        execute {
            val text1 = text.trim()
            GSON.fromJsonObject<RssSource>(text1)?.let {
                finally.invoke(it)
            }
        }.onError {
            toast(it.msg)
        }
    }

}