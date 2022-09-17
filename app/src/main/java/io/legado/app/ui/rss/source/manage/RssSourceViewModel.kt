package io.legado.app.ui.rss.source.manage

import android.app.Application
import android.text.TextUtils
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssSource
import io.legado.app.help.DefaultData
import io.legado.app.utils.*
import java.io.File

/**
 * 订阅源管理数据修改
 * 修改数据要copy,直接修改会导致界面不刷新
 */
class RssSourceViewModel(application: Application) : BaseViewModel(application) {

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

    fun update(vararg rssSource: RssSource) {
        execute { appDb.rssSourceDao.update(*rssSource) }
    }

    fun upOrder() {
        execute {
            val sources = appDb.rssSourceDao.all
            for ((index: Int, source: RssSource) in sources.withIndex()) {
                source.customOrder = index + 1
            }
            appDb.rssSourceDao.update(*sources.toTypedArray())
        }
    }

    fun enableSelection(sources: List<RssSource>) {
        execute {
            val array = Array(sources.size) {
                sources[it].copy(enabled = true)
            }
            appDb.rssSourceDao.update(*array)
        }
    }

    fun disableSelection(sources: List<RssSource>) {
        execute {
            val array = Array(sources.size) {
                sources[it].copy(enabled = false)
            }
            appDb.rssSourceDao.update(*array)
        }
    }

    fun saveToFile(sources: List<RssSource>, success: (file: File) -> Unit) {
        execute {
            val path = "${context.filesDir}/shareRssSource.json"
            FileUtils.delete(path)
            val file = FileUtils.createFileWithReplace(path)
            file.writeText(GSON.toJson(sources))
            file
        }.onSuccess {
            success.invoke(it)
        }.onError {
            context.toastOnUi(it.stackTraceStr)
        }
    }

    fun selectionAddToGroups(sources: List<RssSource>, groups: String) {
        execute {
            val array = Array(sources.size) {
                sources[it].copy().addGroup(groups)
            }
            appDb.rssSourceDao.update(*array)
        }
    }

    fun selectionRemoveFromGroups(sources: List<RssSource>, groups: String) {
        execute {
            val array = Array(sources.size) {
                sources[it].copy().removeGroup(groups)
            }
            appDb.rssSourceDao.update(*array)
        }
    }

    fun addGroup(group: String) {
        execute {
            val sources = appDb.rssSourceDao.noGroup
            sources.map { source ->
                source.sourceGroup = group
            }
            appDb.rssSourceDao.update(*sources.toTypedArray())
        }
    }

    fun upGroup(oldGroup: String, newGroup: String?) {
        execute {
            val sources = appDb.rssSourceDao.getByGroup(oldGroup)
            sources.map { source ->
                source.sourceGroup?.splitNotBlank(",")?.toHashSet()?.let {
                    it.remove(oldGroup)
                    if (!newGroup.isNullOrEmpty())
                        it.add(newGroup)
                    source.sourceGroup = TextUtils.join(",", it)
                }
            }
            appDb.rssSourceDao.update(*sources.toTypedArray())
        }
    }

    fun delGroup(group: String) {
        execute {
            execute {
                val sources = appDb.rssSourceDao.getByGroup(group)
                sources.map { source ->
                    source.sourceGroup?.splitNotBlank(",")?.toHashSet()?.let {
                        it.remove(group)
                        source.sourceGroup = TextUtils.join(",", it)
                    }
                }
                appDb.rssSourceDao.update(*sources.toTypedArray())
            }
        }
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultRssSources()
        }
    }

    fun disable(rssSource: RssSource) {
        execute {
            rssSource.enabled = false
            appDb.rssSourceDao.update(rssSource)
        }
    }

}