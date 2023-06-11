package io.legado.app.ui.book.source.manage

import android.app.Application
import android.text.TextUtils
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.help.config.SourceConfig
import io.legado.app.utils.*
import java.io.File
import java.io.FileOutputStream

/**
 * 书源管理数据修改
 * 修改数据要copy,直接修改会导致界面不刷新
 */
class BookSourceViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(vararg sources: BookSourcePart) {
        execute {
            sources.sortBy { it.customOrder }
            val minOrder = appDb.bookSourceDao.minOrder - 1
            val array = sources.mapIndexed { index, it ->
                it.copy(customOrder = minOrder - index)
            }
            appDb.bookSourceDao.upOrder(array)
        }
    }

    fun bottomSource(vararg sources: BookSourcePart) {
        execute {
            sources.sortBy { it.customOrder }
            val maxOrder = appDb.bookSourceDao.maxOrder + 1
            val array = sources.mapIndexed { index, it ->
                it.copy(customOrder = maxOrder + index)
            }
            appDb.bookSourceDao.upOrder(array)
        }
    }

    fun del(sources: List<BookSourcePart>) {
        execute {
            appDb.bookSourceDao.delete(sources)
            sources.forEach {
                SourceConfig.removeSource(it.bookSourceUrl)
            }
        }
    }

    fun update(vararg bookSource: BookSource) {
        execute { appDb.bookSourceDao.update(*bookSource) }
    }

    fun upOrder(items: List<BookSourcePart>) {
        if (items.isEmpty()) return
        execute {
            val firstSortNumber = items[0].customOrder
            items.forEachIndexed { index, bookSource ->
                bookSource.customOrder = firstSortNumber + index
            }
            appDb.bookSourceDao.upOrder(items)
        }
    }

    fun enable(enable: Boolean, items: List<BookSourcePart>) {
        execute {
            appDb.bookSourceDao.enable(enable, items)
        }
    }

    fun enableSelection(sources: List<BookSourcePart>) {
        execute {
            appDb.bookSourceDao.enable(true, sources)
        }
    }

    fun disableSelection(sources: List<BookSourcePart>) {
        execute {
            appDb.bookSourceDao.enable(false, sources)
        }
    }

    fun enableExplore(enable: Boolean, items: List<BookSourcePart>) {
        execute {
            appDb.bookSourceDao.enableExplore(enable, items)
        }
    }

    fun enableSelectExplore(sources: List<BookSourcePart>) {
        execute {
            appDb.bookSourceDao.enableExplore(true, sources)
        }
    }

    fun disableSelectExplore(sources: List<BookSourcePart>) {
        execute {
            appDb.bookSourceDao.enableExplore(false, sources)
        }
    }

    fun selectionAddToGroups(sources: List<BookSourcePart>, groups: String) {
        execute {
            val array = sources.map {
                it.copy().apply {
                    addGroup(groups)
                }
            }
            appDb.bookSourceDao.upGroup(array)
        }
    }

    fun selectionRemoveFromGroups(sources: List<BookSourcePart>, groups: String) {
        execute {
            val array = sources.map {
                it.copy().apply {
                    removeGroup(groups)
                }
            }
            appDb.bookSourceDao.upGroup(array)
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