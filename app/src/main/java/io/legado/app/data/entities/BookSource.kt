package io.legado.app.data.entities

import android.os.Parcelable
import android.text.TextUtils
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import io.legado.app.constant.AppPattern
import io.legado.app.constant.BookSourceType
import io.legado.app.data.entities.rule.BookInfoRule
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.data.entities.rule.ExploreRule
import io.legado.app.data.entities.rule.ReviewRule
import io.legado.app.data.entities.rule.SearchRule
import io.legado.app.data.entities.rule.TocRule
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.splitNotBlank
import kotlinx.parcelize.Parcelize

@Suppress("unused")
@Parcelize
@TypeConverters(BookSource.Converters::class)
@Entity(
    tableName = "book_sources",
    indices = [(Index(value = ["bookSourceUrl"], unique = false))]
)
data class BookSource(
    // 地址，包括 http/https
    @PrimaryKey
    var bookSourceUrl: String = "",
    // 名称
    var bookSourceName: String = "",
    // 分组
    var bookSourceGroup: String? = null,
    // 类型，0 文本，1 音频, 2 图片, 3 文件（指的是类似知轩藏书只提供下载的网站）
    @BookSourceType.Type
    var bookSourceType: Int = 0,
    // 详情页url正则
    var bookUrlPattern: String? = null,
    // 手动排序编号
    @ColumnInfo(defaultValue = "0")
    var customOrder: Int = 0,
    // 是否启用
    @ColumnInfo(defaultValue = "1")
    var enabled: Boolean = true,
    // 启用发现
    @ColumnInfo(defaultValue = "1")
    var enabledExplore: Boolean = true,
    // js库
    override var jsLib: String? = null,
    // 启用okhttp CookieJAr 自动保存每次请求的cookie
    @ColumnInfo(defaultValue = "0")
    override var enabledCookieJar: Boolean? = true,
    // 并发率
    override var concurrentRate: String? = null,
    // 请求头
    override var header: String? = null,
    // 登录地址
    override var loginUrl: String? = null,
    // 登录UI
    override var loginUi: String? = null,
    // 登录检测js
    var loginCheckJs: String? = null,
    // 封面解密js
    var coverDecodeJs: String? = null,
    // 注释
    var bookSourceComment: String? = null,
    // 自定义变量说明
    var variableComment: String? = null,
    // 最后更新时间，用于排序
    var lastUpdateTime: Long = 0,
    // 响应时间，用于排序
    var respondTime: Long = 180000L,
    // 智能排序的权重
    var weight: Int = 0,
    // 发现url
    var exploreUrl: String? = null,
    // 发现筛选规则
    var exploreScreen: String? = null,
    // 发现规则
    var ruleExplore: ExploreRule? = null,
    // 搜索url
    var searchUrl: String? = null,
    // 搜索规则
    var ruleSearch: SearchRule? = null,
    // 书籍信息页规则
    var ruleBookInfo: BookInfoRule? = null,
    // 目录页规则
    var ruleToc: TocRule? = null,
    // 正文页规则
    var ruleContent: ContentRule? = null,
    // 段评规则
    var ruleReview: ReviewRule? = null
) : Parcelable, BaseSource {

    override fun getTag(): String {
        return bookSourceName
    }

    override fun getKey(): String {
        return bookSourceUrl
    }

    override fun hashCode(): Int {
        return bookSourceUrl.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is BookSource) other.bookSourceUrl == bookSourceUrl else false
    }

    fun getSearchRule(): SearchRule {
        ruleSearch?.let { return it }
        val rule = SearchRule()
        ruleSearch = rule
        return rule
    }

    fun getExploreRule(): ExploreRule {
        ruleExplore?.let { return it }
        val rule = ExploreRule()
        ruleExplore = rule
        return rule
    }

    fun getBookInfoRule(): BookInfoRule {
        ruleBookInfo?.let { return it }
        val rule = BookInfoRule()
        ruleBookInfo = rule
        return rule
    }

    fun getTocRule(): TocRule {
        ruleToc?.let { return it }
        val rule = TocRule()
        ruleToc = rule
        return rule
    }

    fun getContentRule(): ContentRule {
        ruleContent?.let { return it }
        val rule = ContentRule()
        ruleContent = rule
        return rule
    }

    fun getReviewRule(): ReviewRule {
        ruleReview?.let { return it }
        val rule = ReviewRule()
        ruleReview = rule
        return rule
    }

    fun getDisPlayNameGroup(): String {
        return if (bookSourceGroup.isNullOrBlank()) {
            bookSourceName
        } else {
            String.format("%s (%s)", bookSourceName, bookSourceGroup)
        }
    }

    fun addGroup(groups: String): BookSource {
        bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.toHashSet()?.let {
            it.addAll(groups.splitNotBlank(AppPattern.splitGroupRegex))
            bookSourceGroup = TextUtils.join(",", it)
        }
        if (bookSourceGroup.isNullOrBlank()) bookSourceGroup = groups
        return this
    }

    fun removeGroup(groups: String): BookSource {
        bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.toHashSet()?.let {
            it.removeAll(groups.splitNotBlank(AppPattern.splitGroupRegex).toSet())
            bookSourceGroup = TextUtils.join(",", it)
        }
        return this
    }

    fun hasGroup(group: String): Boolean {
        bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.toHashSet()?.let {
            return it.indexOf(group) != -1
        }
        return false
    }

    fun removeInvalidGroups() {
        removeGroup(getInvalidGroupNames())
    }

    fun removeErrorComment() {
        bookSourceComment = bookSourceComment
            ?.split("\n\n")
            ?.filterNot {
                it.startsWith("// Error: ")
            }?.joinToString("\n")
    }

    fun addErrorComment(e: Throwable) {
        bookSourceComment =
            "// Error: ${e.localizedMessage}" + if (bookSourceComment.isNullOrBlank())
                "" else "\n\n${bookSourceComment}"
    }

    fun getCheckKeyword(default: String): String {
        ruleSearch?.checkKeyWord?.let {
            if (it.isNotBlank()) {
                return it
            }
        }
        return default
    }

    fun getInvalidGroupNames(): String {
        return bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.toHashSet()?.filter {
            "失效" in it || it == "校验超时"
        }?.joinToString() ?: ""
    }

    fun getDisplayVariableComment(otherComment: String): String {
        return if (variableComment.isNullOrBlank()) {
            otherComment
        } else {
            "${variableComment}\n$otherComment"
        }
    }

    fun equal(source: BookSource): Boolean {
        return equal(bookSourceName, source.bookSourceName)
                && equal(bookSourceUrl, source.bookSourceUrl)
                && equal(bookSourceGroup, source.bookSourceGroup)
                && bookSourceType == source.bookSourceType
                && equal(bookUrlPattern, source.bookUrlPattern)
                && equal(bookSourceComment, source.bookSourceComment)
                && customOrder == source.customOrder
                && enabled == source.enabled
                && enabledExplore == source.enabledExplore
                && enabledCookieJar == source.enabledCookieJar
                && equal(variableComment, source.variableComment)
                && equal(concurrentRate, source.concurrentRate)
                && equal(jsLib, source.jsLib)
                && equal(header, source.header)
                && equal(loginUrl, source.loginUrl)
                && equal(loginUi, source.loginUi)
                && equal(loginCheckJs, source.loginCheckJs)
                && equal(coverDecodeJs, source.coverDecodeJs)
                && equal(exploreUrl, source.exploreUrl)
                && equal(searchUrl, source.searchUrl)
                && getSearchRule() == source.getSearchRule()
                && getExploreRule() == source.getExploreRule()
                && getBookInfoRule() == source.getBookInfoRule()
                && getTocRule() == source.getTocRule()
                && getContentRule() == source.getContentRule()
    }

    private fun equal(a: String?, b: String?) = a == b || (a.isNullOrEmpty() && b.isNullOrEmpty())

    class Converters {

        @TypeConverter
        fun exploreRuleToString(exploreRule: ExploreRule?): String =
            GSON.toJson(exploreRule)

        @TypeConverter
        fun stringToExploreRule(json: String?) =
            GSON.fromJsonObject<ExploreRule>(json).getOrNull()

        @TypeConverter
        fun searchRuleToString(searchRule: SearchRule?): String =
            GSON.toJson(searchRule)

        @TypeConverter
        fun stringToSearchRule(json: String?) =
            GSON.fromJsonObject<SearchRule>(json).getOrNull()

        @TypeConverter
        fun bookInfoRuleToString(bookInfoRule: BookInfoRule?): String =
            GSON.toJson(bookInfoRule)

        @TypeConverter
        fun stringToBookInfoRule(json: String?) =
            GSON.fromJsonObject<BookInfoRule>(json).getOrNull()

        @TypeConverter
        fun tocRuleToString(tocRule: TocRule?): String =
            GSON.toJson(tocRule)

        @TypeConverter
        fun stringToTocRule(json: String?) =
            GSON.fromJsonObject<TocRule>(json).getOrNull()

        @TypeConverter
        fun contentRuleToString(contentRule: ContentRule?): String =
            GSON.toJson(contentRule)

        @TypeConverter
        fun stringToContentRule(json: String?) =
            GSON.fromJsonObject<ContentRule>(json).getOrNull()

        @TypeConverter
        fun stringToReviewRule(json: String?): ReviewRule? = null

        @TypeConverter
        fun reviewRuleToString(reviewRule: ReviewRule?): String = "null"

    }
}