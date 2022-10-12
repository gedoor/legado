package io.legado.app.ui.book.search

import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.splitNotBlank

/**
 * 搜索范围
 */
@Suppress("unused")
data class SearchScope(var scope: String) {

    constructor(groups: List<String>) : this(groups.joinToString(","))

    constructor(source: BookSource) : this("${source.bookSourceName}::${source.bookSourceUrl}")

    override fun toString(): String {
        return scope
    }

    /**
     * 搜索范围显示
     */
    fun getShowNames(): List<String> {
        val list = arrayListOf<String>()
        if (scope.contains("::")) {
            list.add(scope.substringBefore("::"))
        } else {
            scope.splitNotBlank(",").forEach {
                list.add(it)
            }
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
        if (scope.contains("::")) {
            scope.substringAfter("::").let {
                appDb.bookSourceDao.getBookSource(it)?.let { source ->
                    list.add(source)
                }
            }
        } else {
            scope.splitNotBlank(",").forEach {
                list.addAll(appDb.bookSourceDao.getByGroup(it))
            }
        }
        if (list.isEmpty()) {
            scope = ""
            return appDb.bookSourceDao.allEnabled
        }
        return list.sortedBy { it.customOrder }
    }

    fun save() {
        AppConfig.searchScope = scope
    }

}
