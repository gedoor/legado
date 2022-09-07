package io.legado.app.ui.book.source.manage

import android.app.Application
import android.text.TextUtils
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.help.config.SourceConfig
import io.legado.app.utils.*
import java.io.File
import java.io.FileOutputStream

/**
 * 书源管理数据修改
 * 修改数据要copy,直接修改会导致界面不刷新
 */
class BookSourceViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(vararg sources: BookSource) {
        execute {
            sources.sortBy { it.customOrder }
            val minOrder = appDb.bookSourceDao.minOrder - 1
            val array = Array(sources.size) {
                sources[it].copy(customOrder = minOrder - it)
            }
            appDb.bookSourceDao.update(*array)
        }
    }

    fun bottomSource(vararg sources: BookSource) {
        execute {
            sources.sortBy { it.customOrder }
            val maxOrder = appDb.bookSourceDao.maxOrder + 1
            val array = Array(sources.size) {
                sources[it].copy(customOrder = maxOrder + it)
            }
            appDb.bookSourceDao.update(*array)
        }
    }

    fun del(vararg sources: BookSource) {
        execute {
            appDb.bookSourceDao.delete(*sources)
            sources.forEach {
                SourceConfig.removeSource(it.bookSourceUrl)
            }
        }
    }

    fun update(vararg bookSource: BookSource) {
        execute { appDb.bookSourceDao.update(*bookSource) }
    }

    fun upOrder() {
        execute {
            val sources = appDb.bookSourceDao.all
            for ((index: Int, source: BookSource) in sources.withIndex()) {
                source.customOrder = index + 1
            }
            appDb.bookSourceDao.update(*sources.toTypedArray())
        }
    }

    fun enableSelection(sources: List<BookSource>) {
        execute {
            val array = Array(sources.size) {
                sources[it].copy(enabled = true)
            }
            appDb.bookSourceDao.update(*array)
        }
    }

    fun disableSelection(sources: List<BookSource>) {
        execute {
            val array = Array(sources.size) {
                sources[it].copy(enabled = false)
            }
            appDb.bookSourceDao.update(*array)
        }
    }

    fun enableSelectExplore(sources: List<BookSource>) {
        execute {
            val array = Array(sources.size) {
                sources[it].copy(enabledExplore = true)
            }
            appDb.bookSourceDao.update(*array)
        }
    }

    fun disableSelectExplore(sources: List<BookSource>) {
        execute {
            val array = Array(sources.size) {
                sources[it].copy(enabledExplore = false)
            }
            appDb.bookSourceDao.update(*array)
        }
    }

    fun selectionAddToGroups(sources: List<BookSource>, groups: String) {
        execute {
            val array = Array(sources.size) {
                sources[it].copy().addGroup(groups)
            }
            appDb.bookSourceDao.update(*array)
        }
    }

    fun selectionRemoveFromGroups(sources: List<BookSource>, groups: String) {
        execute {
            val array = Array(sources.size) {
                sources[it].copy().removeGroup(groups)
            }
            appDb.bookSourceDao.update(*array)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun saveToFile(sources: List<BookSource>, success: (file: File) -> Unit) {
        execute {
            val path = "${context.filesDir}/shareBookSource.json"
            FileUtils.delete(path)
            val file = FileUtils.createFileWithReplace(path)
            FileOutputStream(file).use {
                GSON.writeToOutputStream(it, sources)
            }
            file
        }.onSuccess {
            success.invoke(it)
        }.onError {
            context.toastOnUi(it.stackTraceStr)
        }
    }

    fun addGroup(group: String) {
        execute {
            val sources = appDb.bookSourceDao.noGroup
            sources.map { source ->
                source.bookSourceGroup = group
            }
            appDb.bookSourceDao.update(*sources.toTypedArray())
        }
    }

    fun upGroup(oldGroup: String, newGroup: String?) {
        execute {
            val sources = appDb.bookSourceDao.getByGroup(oldGroup)
            sources.map { source ->
                source.bookSourceGroup?.splitNotBlank(",")?.toHashSet()?.let {
                    it.remove(oldGroup)
                    if (!newGroup.isNullOrEmpty())
                        it.add(newGroup)
                    source.bookSourceGroup = TextUtils.join(",", it)
                }
            }
            appDb.bookSourceDao.update(*sources.toTypedArray())
        }
    }

    fun delGroup(group: String) {
        execute {
            execute {
                val sources = appDb.bookSourceDao.getByGroup(group)
                sources.map { source ->
                    source.removeGroup(group)
                }
                appDb.bookSourceDao.update(*sources.toTypedArray())
            }
        }
    }

}