package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import io.legado.app.App
import io.legado.app.constant.AppConst.userAgent
import io.legado.app.data.entities.rule.*
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.getPrefString
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.util.*

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
    @Ignore
    @IgnoredOnParcel
    var searchRuleV:SearchRule? = null

    @Ignore
    @IgnoredOnParcel
    var exploreRuleV:ExploreRule? = null

    @Ignore
    @IgnoredOnParcel
    var bookInfoRuleV:BookInfoRule? = null

    @Ignore
    @IgnoredOnParcel
    var tocRuleV:TocRule? = null

    @Ignore
    @IgnoredOnParcel
    var contentRuleV:ContentRule? = null

    fun getHeaderMap(): Map<String, String> {
        val headerMap = HashMap<String, String>()
        headerMap["user_agent"] = App.INSTANCE.getPrefString("user_agent") ?: userAgent
        header?.let {
            GSON.fromJsonObject<Map<String, String>>(header)?.let {
                headerMap.putAll(it)
            }
        }
        return headerMap
    }


    fun getSearchRule(): SearchRule {
        searchRuleV?:let {
            searchRuleV = GSON.fromJsonObject<SearchRule>(ruleSearch)
            searchRuleV?:let { searchRuleV = SearchRule() }
        }
        return searchRuleV!!
    }

    fun getExploreRule(): ExploreRule {
        exploreRuleV?:let {
            exploreRuleV = GSON.fromJsonObject<ExploreRule>(ruleExplore)
            exploreRuleV?:let { exploreRuleV = ExploreRule() }
        }
        return exploreRuleV!!
    }

    fun getBookInfoRule(): BookInfoRule? {
        bookInfoRuleV?:let {
            bookInfoRuleV = GSON.fromJsonObject<BookInfoRule>(ruleBookInfo)
            bookInfoRuleV?:let { bookInfoRuleV = BookInfoRule() }
        }
        return bookInfoRuleV!!
    }

    fun getTocRule(): TocRule? {
        tocRuleV?:let {
            tocRuleV = GSON.fromJsonObject<TocRule>(ruleToc)
            tocRuleV?:let { tocRuleV = TocRule() }
        }
        return tocRuleV!!
    }

    fun getContentRule(): ContentRule? {
        contentRuleV?:let {
            contentRuleV = GSON.fromJsonObject<ContentRule>(ruleContent)
            contentRuleV?:let { contentRuleV = ContentRule() }
        }
        return contentRuleV!!
    }
}