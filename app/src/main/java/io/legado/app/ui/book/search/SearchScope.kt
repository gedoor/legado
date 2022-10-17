package io.legado.app.ui.book.search

import androidx.lifecycle.MutableLiveData
import io.legado.app.R
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.splitNotBlank
import splitties.init.appCtx

/**
 * 搜索范围
 */
@Suppress("unused")
data class SearchScope(private var scope: String) {

    constructor(groups: List<String>) : this(groups.joinToString(","))

    constructor(source: BookSource) : this(
        "${source.bookSourceName.replace(":", "")}::${source.bookSourceUrl}"
    )

    override fun toString(): String {
        return scope
    }

    val stateLiveData = MutableLiveData("")

    fun update(scope: String) {
        this.scope = scope
        stateLiveData.postValue(scope)
    }

    fun update(groups: List<String>) {
        scope = groups.joinToString(",")
    }

    fun update(source: BookSource) {
        scope = "${source.bookSourceName}::${source.bookSourceUrl}"
    }

    val display: String
        get() {
            if (scope.contains("::")) {
                return scope.substringBefore("::")
            }
            if (scope.isEmpty()) {
                return appCtx.getString(R.string.all_source)
            }
            return scope
        }

    /**
     * 搜索范围显示
     */
    val displayNames: List<String>
        get() {
            val list = arrayListOf<String>()
            if (scope.contains("::")) {
                list.add(scope.substringBefore("::"))
            } else {
                scope.splitNotBlank(",").forEach {
                    list.add(it)
                }
            }
            if (list.isEmpty()) {
                list.add(appCtx.getString(R.string.all_source))
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
            val oldScope = scope.splitNotBlank(",")
            val newScope = oldScope.filter {
                val bookSources = appDb.bookSourceDao.getEnabledByGroup(it)
                list.addAll(bookSources)
                bookSources.isNotEmpty()
            }
            if (oldScope.size != newScope.size) {
                update(newScope)
                stateLiveData.postValue(scope)
            }
        }
        if (list.isEmpty()) {
            scope = ""
            stateLiveData.postValue(scope)
            return appDb.bookSourceDao.allEnabled
        }
        return list.sortedBy { it.customOrder }
    }

    fun isAll(): Boolean {
        return scope.isEmpty()
    }

    fun save() {
        AppConfig.searchScope = scope
    }

}
