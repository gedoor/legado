package io.legado.app.ui.rss.source.manage

import android.app.Application
import android.text.TextUtils
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssSource
import io.legado.app.utils.FileUtils
import io.legado.app.utils.GSON
import io.legado.app.utils.splitNotBlank
import io.legado.app.utils.writeText
import org.jetbrains.anko.toast
import java.io.File

class RssSourceViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(vararg sources: RssSource) {
        execute {
            val minOrder = App.db.rssSourceDao().minOrder - 1
            sources.forEachIndexed { index, rssSource ->
                rssSource.customOrder = minOrder - index
            }
            App.db.rssSourceDao().update(*sources)
        }
    }

    fun bottomSource(vararg sources: RssSource) {
        execute {
            val maxOrder = App.db.rssSourceDao().maxOrder + 1
            sources.forEachIndexed { index, rssSource ->
                rssSource.customOrder = maxOrder + index
            }
            App.db.rssSourceDao().update(*sources)
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

    fun enableSelection(sources: List<RssSource>) {
        execute {
            val list = arrayListOf<RssSource>()
            sources.forEach {
                list.add(it.copy(enabled = true))
            }
            App.db.rssSourceDao().update(*list.toTypedArray())
        }
    }

    fun disableSelection(sources: List<RssSource>) {
        execute {
            val list = arrayListOf<RssSource>()
            sources.forEach {
                list.add(it.copy(enabled = false))
            }
            App.db.rssSourceDao().update(*list.toTypedArray())
        }
    }

    fun delSelection(sources: List<RssSource>) {
        execute {
            App.db.rssSourceDao().delete(*sources.toTypedArray())
        }
    }

    fun exportSelection(sources: List<RssSource>, file: File) {
        execute {
            val json = GSON.toJson(sources)
            FileUtils.createFileIfNotExist(file, "exportRssSource.json")
                .writeText(json)
        }.onSuccess {
            context.toast("成功导出至\n${file.absolutePath}")
        }.onError {
            context.toast("导出失败\n${it.localizedMessage}")
        }
    }

    fun exportSelection(sources: List<RssSource>, doc: DocumentFile) {
        execute {
            val json = GSON.toJson(sources)
            doc.findFile("exportRssSource.json")?.delete()
            doc.createFile("", "exportRssSource.json")
                ?.writeText(context, json)
        }.onSuccess {
            context.toast("成功导出至\n${doc.uri.path}")
        }.onError {
            context.toast("导出失败\n${it.localizedMessage}")
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

}