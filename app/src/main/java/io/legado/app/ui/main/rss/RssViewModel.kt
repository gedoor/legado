package io.legado.app.ui.main.rss

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssSource
import io.legado.app.utils.toastOnUi

class RssViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(vararg sources: RssSource) {
        execute {
            sources.sortBy { it.customOrder }
            val minOrder = appDb.rssSourceDao.minOrder - 1
            val array = Array(sources.size) {
                sources[it].copy(customOrder = minOrder - it)
            }
            appDb.rssSourceDao.update(*array)
        }
    }

    fun bottomSource(vararg sources: RssSource) {
        execute {
            sources.sortBy { it.customOrder }
            val maxOrder = appDb.rssSourceDao.maxOrder + 1
            val array = Array(sources.size) {
                sources[it].copy(customOrder = maxOrder + it)
            }
            appDb.rssSourceDao.update(*array)
        }
    }

    fun del(vararg rssSource: RssSource) {
        execute { appDb.rssSourceDao.delete(*rssSource) }
    }

    fun disable(rssSource: RssSource) {
        execute {
            rssSource.enabled = false
            appDb.rssSourceDao.update(rssSource)
        }
    }

    fun getSingleUrl(rssSource: RssSource, onSuccess: (url: String) -> Unit) {
        execute {
            val url = rssSource.sortUrl
            if (!url.isNullOrBlank()) {
                if (url.startsWith("<js>", false)
                    || url.startsWith("@js:", false)
                ) {
                    val jsStr = if (url.startsWith("@")) {
                        url.substring(4)
                    } else {
                        url.substring(4, url.lastIndexOf("<"))
                    }
                    val result = rssSource.evalJS(jsStr)?.toString()
                    if (!result.isNullOrBlank()) {
                        return@execute result
                    }
                } else {
                    return@execute url
                }
            }
            rssSource.sourceUrl
        }.timeout(10000)
            .onSuccess {
                onSuccess.invoke(it)
            }.onError {
                context.toastOnUi(it.localizedMessage)
            }
    }


}