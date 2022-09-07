package io.legado.app.ui.rss.source.edit

import android.app.Application
import android.content.Intent
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssSource
import io.legado.app.help.RuleComplete
import io.legado.app.help.http.CookieStore
import io.legado.app.utils.getClipText
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.stackTraceStr

import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers


class RssSourceEditViewModel(application: Application) : BaseViewModel(application) {
    var autoComplete = false
    var rssSource: RssSource = RssSource()
    private var oldSourceUrl: String = ""

    fun initData(intent: Intent, onFinally: () -> Unit) {
        execute {
            val key = intent.getStringExtra("sourceUrl")
            if (key != null) {
                appDb.rssSourceDao.getByKey(key)?.let {
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
                appDb.rssSourceDao.delete(oldSourceUrl)
                oldSourceUrl = source.sourceUrl
            }
            appDb.rssSourceDao.insert(source)
        }.onSuccess {
            success()
        }.onError {
            context.toastOnUi(it.localizedMessage)
            it.printOnDebug()
        }
    }

    fun pasteSource(onSuccess: (source: RssSource) -> Unit) {
        execute(context = Dispatchers.Main) {
            var source: RssSource? = null
            context.getClipText()?.let { json ->
                source = RssSource.fromJson(json).getOrThrow()
            }
            source
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }.onSuccess {
            if (it != null) {
                onSuccess(it)
            } else {
                context.toastOnUi("格式不对")
            }
        }
    }

    fun importSource(text: String, finally: (source: RssSource) -> Unit) {
        execute {
            val text1 = text.trim()
            RssSource.fromJson(text1).getOrThrow().let {
                finally.invoke(it)
            }
        }.onError {
            context.toastOnUi(it.stackTraceStr)
        }
    }

    fun clearCookie(url: String) {
        execute {
            CookieStore.removeCookie(url)
        }
    }

    fun ruleComplete(rule: String?, preRule: String? = null, type: Int = 1): String? {
        if (autoComplete) {
            return RuleComplete.autoComplete(rule, preRule, type)
        }
        return rule
    }

}