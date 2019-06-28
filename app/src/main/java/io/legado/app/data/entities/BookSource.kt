package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.legado.app.data.entities.rule.*
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(
    tableName = "book_sources",
    indices = [(Index(value = ["bookSourceUrl"], unique = false))]
)
data class BookSource(
    var bookSourceName: String = "",                    // 名称
    var bookSourceGroup: String? = null,                // 分组
    @PrimaryKey
    var bookSourceUrl: String = "",                  // 地址，包括 http/https
    var bookSourceType: Int = 0,                        // 类型，0 文本，1 音频
    var customOrder: Int = 0,                 // 手动排序编号
    var enabled: Boolean = true,            // 是否启用
    var enabledExplore: Boolean = true,     //启用发现
    var header: String? = null,               // header
    var loginUrl: String? = null,             // 登录地址
    var lastUpdateTime: Long = 0,             // 最后更新时间，用于排序
    var weight: Int = 0,                      // 智能排序的权重
    var ruleExplore: String? = null,          // 发现规则
    var ruleSearch: String? = null,           // 搜索规则
    var ruleBookInfo: String? = null,         // 书籍信息页规则
    var ruleToc: String? = null,          // 目录页规则
    var ruleContent: String? = null           // 正文页规则
) : Parcelable {

    fun getSearchRule(): SearchRule? {
        return GSON.fromJsonObject<SearchRule>(ruleSearch)
    }

    fun getExploreRule(): ExploreRule? {
        return GSON.fromJsonObject<ExploreRule>(ruleExplore)
    }

    fun getBookInfoRule(): BookInfoRule? {
        return GSON.fromJsonObject<BookInfoRule>(ruleBookInfo)
    }

    fun getTocRule(): TocRule? {
        return GSON.fromJsonObject<TocRule>(ruleToc)
    }

    fun getContentRule(): ContentRule? {
        return GSON.fromJsonObject<ContentRule>(ruleContent)
    }
}