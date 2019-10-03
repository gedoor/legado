package io.legado.app.ui.rss.source.manage

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssSource

class RssSourceViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(rssSource: RssSource) {
        execute {
            rssSource.customOrder = App.db.rssSourceDao().minOrder - 1
            App.db.rssSourceDao().insert(rssSource)
        }
    }

    fun del(rssSource: RssSource) {
        execute { App.db.rssSourceDao().delete(rssSource) }
    }

    fun update(vararg rssSource: RssSource) {
        execute { App.db.rssSourceDao().update(*rssSource) }
    }

    fun upOrder() {
        execute {
            val sources = App.db.rssSourceDao().all
            for ((index: Int, source: RssSource) in sources.withIndex()) {
                source.customOrder = index + 1
            }
            App.db.rssSourceDao().update(*sources.toTypedArray())
        }
    }

    fun enableSelection(ids: LinkedHashSet<String>) {
        execute {
            App.db.rssSourceDao().enableSection(*ids.toTypedArray())
        }
    }

    fun disableSelection(ids: LinkedHashSet<String>) {
        execute {
            App.db.rssSourceDao().disableSection(*ids.toTypedArray())
        }
    }

    fun delSelection(ids: LinkedHashSet<String>) {
        execute {
            App.db.rssSourceDao().delSection(*ids.toTypedArray())
        }
    }
}