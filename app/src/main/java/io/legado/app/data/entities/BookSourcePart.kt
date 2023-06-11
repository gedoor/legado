package io.legado.app.data.entities

import android.text.TextUtils
import io.legado.app.constant.AppPattern
import io.legado.app.data.appDb
import io.legado.app.utils.splitNotBlank


data class BookSourcePart(
    // 地址，包括 http/https
    var bookSourceUrl: String = "",
    // 名称
    var bookSourceName: String = "",
    // 分组
    var bookSourceGroup: String? = null,
    // 手动排序编号
    var customOrder: Int = 0,
    // 是否启用
    var enabled: Boolean = true,
    // 启用发现
    var enabledExplore: Boolean = true,
    // 是否有登录地址
    var hasLoginUrl: Boolean = false,
    // 最后更新时间，用于排序
    var lastUpdateTime: Long = 0,
    // 响应时间，用于排序
    var respondTime: Long = 180000L,
    // 智能排序的权重
    var weight: Int = 0,
    // 是否有发现url
    var hasExploreUrl: Boolean = false
) {

    override fun hashCode(): Int {
        return bookSourceUrl.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is BookSourcePart) other.bookSourceUrl == bookSourceUrl else false
    }

    fun getDisPlayNameGroup(): String {
        return if (bookSourceGroup.isNullOrBlank()) {
            bookSourceName
        } else {
            String.format("%s (%s)", bookSourceName, bookSourceGroup)
        }
    }

    fun getBookSource(): BookSource? {
        return appDb.bookSourceDao.getBookSource(bookSourceUrl)
    }

    fun addGroup(groups: String) {
        bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.toHashSet()?.let {
            it.addAll(groups.splitNotBlank(AppPattern.splitGroupRegex))
            bookSourceGroup = TextUtils.join(",", it)
        }
        if (bookSourceGroup.isNullOrBlank()) bookSourceGroup = groups
    }

    fun removeGroup(groups: String) {
        bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.toHashSet()?.let {
            it.removeAll(groups.splitNotBlank(AppPattern.splitGroupRegex).toSet())
            bookSourceGroup = TextUtils.join(",", it)
        }
    }

}

fun List<BookSourcePart>.toBookSource(): List<BookSource> {
    return mapNotNull { it.getBookSource() }
}
