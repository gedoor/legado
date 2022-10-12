package io.legado.app.ui.book.search

import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource

/**
 * 搜索范围
 */
data class SearchScope(
    private val groups: List<String>? = null,
    private val sources: List<BookSource>? = null
) {

    /**
     * 搜索范围显示
     */
    fun getShowNames(): List<String> {
        val list = arrayListOf<String>()
        groups?.let {
            list.addAll(it)
        }
        sources?.forEach {
            list.add(it.bookSourceName)
        }
        if (list.isEmpty()) {
            list.add("全部书源")
        }
        return list
    }

    /**
     * 搜索范围书源
     */
    fun getBookSources(): List<BookSource> {
        val list = hashSetOf<BookSource>()
        sources?.let {
            list.addAll(sources)
        }
        groups?.forEach { group ->
            appDb.bookSourceDao.getEnabledByGroup(group).let {
                list.addAll(it)
            }
        }
        if (list.isEmpty()) {
            return appDb.bookSourceDao.allEnabled
        }
        return list.sortedBy { it.customOrder }
    }

}
