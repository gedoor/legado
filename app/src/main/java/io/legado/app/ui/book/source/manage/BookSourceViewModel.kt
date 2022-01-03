package io.legado.app.ui.book.source.manage

import android.app.Application
import android.text.TextUtils
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppPattern
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.utils.*
import java.io.File
import java.io.FileOutputStream

class BookSourceViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(vararg sources: BookSource) {
        execute {
            val minOrder = appDb.bookSourceDao.minOrder - 1
            sources.forEachIndexed { index, bookSource ->
                bookSource.customOrder = minOrder - index
            }
            appDb.bookSourceDao.update(*sources)
        }
    }

    fun bottomSource(vararg sources: BookSource) {
        execute {
            val maxOrder = appDb.bookSourceDao.maxOrder + 1
            sources.forEachIndexed { index, bookSource ->
                bookSource.customOrder = maxOrder + index
            }
            appDb.bookSourceDao.update(*sources)
        }
    }

    fun del(vararg sources: BookSource) {
        execute { appDb.bookSourceDao.delete(*sources) }
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
            val list = arrayListOf<BookSource>()
            sources.forEach {
                list.add(it.copy(enabled = true))
            }
            appDb.bookSourceDao.update(*list.toTypedArray())
        }
    }

    fun disableSelection(sources: List<BookSource>) {
        execute {
            val list = arrayListOf<BookSource>()
            sources.forEach {
                list.add(it.copy(enabled = false))
            }
            appDb.bookSourceDao.update(*list.toTypedArray())
        }
    }

    fun enableSelectExplore(sources: List<BookSource>) {
        execute {
            val list = arrayListOf<BookSource>()
            sources.forEach {
                list.add(it.copy(enabledExplore = true))
            }
            appDb.bookSourceDao.update(*list.toTypedArray())
        }
    }

    fun disableSelectExplore(sources: List<BookSource>) {
        execute {
            val list = arrayListOf<BookSource>()
            sources.forEach {
                list.add(it.copy(enabledExplore = false))
            }
            appDb.bookSourceDao.update(*list.toTypedArray())
        }
    }

    fun selectionAddToGroups(sources: List<BookSource>, groups: String) {
        execute {
            val list = arrayListOf<BookSource>()
            sources.forEach { source ->
                val newGroupList = arrayListOf<String>()
                source.bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.forEach {
                    newGroupList.add(it)
                }
                groups.splitNotBlank(",", ";", "，").forEach {
                    newGroupList.add(it)
                }
                val lh = LinkedHashSet(newGroupList)
                val newGroup = ArrayList(lh).joinToString(separator = ",")
                list.add(source.copy(bookSourceGroup = newGroup))
            }
            appDb.bookSourceDao.update(*list.toTypedArray())
        }
    }

    fun selectionRemoveFromGroups(sources: List<BookSource>, groups: String) {
        execute {
            val list = arrayListOf<BookSource>()
            sources.forEach { source ->
                val newGroupList = arrayListOf<String>()
                source.bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.forEach {
                    newGroupList.add(it)
                }
                groups.splitNotBlank(",", ";", "，").forEach {
                    newGroupList.remove(it)
                }
                val lh = LinkedHashSet(newGroupList)
                val newGroup = ArrayList(lh).joinToString(separator = ",")
                list.add(source.copy(bookSourceGroup = newGroup))
            }
            appDb.bookSourceDao.update(*list.toTypedArray())
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
            context.toastOnUi(it.msg)
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