package io.legado.app.ui.rss.source.manage

import android.app.Application
import android.text.TextUtils
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssSource
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.splitNotBlank
import java.io.File

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


    fun addGroup(group: String) {
        execute {
            val sources = App.db.rssSourceDao().noGroup
            sources.map { source ->
                source.sourceGroup = group
            }
            App.db.rssSourceDao().update(*sources.toTypedArray())
        }
    }

    fun upGroup(oldGroup: String, newGroup: String?) {
        execute {
            val sources = App.db.rssSourceDao().getByGroup(oldGroup)
            sources.map { source ->
                source.sourceGroup?.splitNotBlank(",")?.toHashSet()?.let {
                    it.remove(oldGroup)
                    if (!newGroup.isNullOrEmpty())
                        it.add(newGroup)
                    source.sourceGroup = TextUtils.join(",", it)
                }
            }
            App.db.rssSourceDao().update(*sources.toTypedArray())
        }
    }

    fun delGroup(group: String) {
        execute {
            execute {
                val sources = App.db.rssSourceDao().getByGroup(group)
                sources.map { source ->
                    source.sourceGroup?.splitNotBlank(",")?.toHashSet()?.let {
                        it.remove(group)
                        source.sourceGroup = TextUtils.join(",", it)
                    }
                }
                App.db.rssSourceDao().update(*sources.toTypedArray())
            }
        }
    }


    fun importSourceFromFilePath(path: String) {
        execute {
            val file = File(path)
            if (file.exists()) {
                GSON.fromJsonArray<RssSource>(file.readText())?.let {
                    App.db.rssSourceDao().insert(*it.toTypedArray())
                }
            }
        }
    }

    fun importSource(sourceStr: String) {

    }
}