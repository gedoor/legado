package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.*
import io.legado.app.constant.AppUtils.GSON_CONVERTER
import io.legado.app.data.entities.rule.*
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(
    tableName = "book_sources",
    indices = [(Index(value = ["sourceId"])), (Index(value = ["origin"], unique = false))]
)
data class BookSource (
    @PrimaryKey
    var origin: String = "",                  // 地址，包括 http/https
    var name: String = "",                    // 名称
    var type: Int = 0,                        // 类型，0 文本，1 音频
    var group: String? = null,                // 分组
    var header: String? = null,               // header
    var loginUrl: String? = null,             // 登录地址
    var isEnabled: Boolean = true,            // 是否启用
    var lastUpdateTime: Long = 0,             // 最后更新时间，用于排序
    var customOrder: Int = 0,                 // 手动排序编号
    var weight: Int = 0,                      // 智能排序的权重
    var exploreIsEnabled: Boolean = true,     //启用发现
    var exploreRule: String? = null,          // 发现规则
    var searchRule: String? = null,           // 搜索规则
    var bookInfoRule: String? = null,         // 书籍信息页规则
    var chapterRule: String? = null,          // 目录页规则
    var contentRule: String? = null           // 正文页规则
) : Parcelable {
    @Transient
    @IgnoredOnParcel
    var parsedExploreRule: ExploreRule? = null
    @Transient
    @IgnoredOnParcel
    var parsedSearchRule: SearchRule? = null
    @Transient
    @IgnoredOnParcel
    var parsedBookInfoRule: BookInfoRule? = null
    @Transient
    @IgnoredOnParcel
    var parsedChapterRule: ChapterRule? = null
    @Transient
    @IgnoredOnParcel
    var parsedContentRule: ContentRule? = null

    fun initAllRules() {
        initSearchRule()
        initExploreRule()
        initBookInfoRule()
        initChapterRule()
        initContentRule()
    }

    fun initSearchRule() {
        if (searchRule != null) {
            parsedSearchRule = GSON_CONVERTER.fromJson(searchRule, SearchRule::class.java)
        }
    }

    fun initExploreRule() {
        if (exploreRule != null) {
            parsedExploreRule = GSON_CONVERTER.fromJson(exploreRule, ExploreRule::class.java)
        }
    }

    fun initBookInfoRule() {
        if (bookInfoRule != null) {
            parsedBookInfoRule = GSON_CONVERTER.fromJson(bookInfoRule, BookInfoRule::class.java)
        }
    }

    fun initChapterRule() {
        if (chapterRule != null) {
            parsedChapterRule = GSON_CONVERTER.fromJson(chapterRule, ChapterRule::class.java)
        }
    }

    fun initContentRule() {
        if (contentRule != null) {
            parsedContentRule = GSON_CONVERTER.fromJson(contentRule, ContentRule::class.java)
        }
    }

}