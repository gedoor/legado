package io.legado.app.ui.rss.source.manage

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel

class RssSourceViewModel(application: Application) : BaseViewModel(application) {

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