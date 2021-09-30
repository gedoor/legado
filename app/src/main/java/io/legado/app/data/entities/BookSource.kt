package io.legado.app.data.entities

import android.os.Parcelable
import android.text.TextUtils
import androidx.room.*
import io.legado.app.constant.BookType
import io.legado.app.data.entities.rule.*
import io.legado.app.utils.*
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import splitties.init.appCtx

@Parcelize
@TypeConverters(BookSource.Converters::class)
@Entity(
    tableName = "book_sources",
    indices = [(Index(value = ["bookSourceUrl"], unique = false))]
)
data class BookSource(
    var bookSourceName: String = "",                // 名称
    var bookSourceGroup: String? = null,            // 分组
    @PrimaryKey
    var bookSourceUrl: String = "",                 // 地址，包括 http/https
    var bookSourceType: Int = BookType.default,     // 类型，0 文本，1 音频, 3 图片
    var bookUrlPattern: String? = null,             // 详情页url正则
    var customOrder: Int = 0,                       // 手动排序编号
    var enabled: Boolean = true,                    // 是否启用
    var enabledExplore: Boolean = true,             // 启用发现
    override var concurrentRate: String? = null,    // 并发率
    override var header: String? = null,            // 请求头
    override var loginUrl: String? = null,          // 登录地址
    override var loginUi: List<RowUi>? = null,      // 登录UI
    var loginCheckJs: String? = null,               // 登录检测js
    var bookSourceComment: String? = null,          // 注释
    var lastUpdateTime: Long = 0,                   // 最后更新时间，用于排序
    var respondTime: Long = 180000L,                // 响应时间，用于排序
    var weight: Int = 0,                            // 智能排序的权重
    var exploreUrl: String? = null,                 // 发现url
    var ruleExplore: ExploreRule? = null,           // 发现规则
    var searchUrl: String? = null,                  // 搜索url
    var ruleSearch: SearchRule? = null,             // 搜索规则
    var ruleBookInfo: BookInfoRule? = null,         // 书籍信息页规则
    var ruleToc: TocRule? = null,                   // 目录页规则
    var ruleContent: ContentRule? = null            // 正文页规则
) : Parcelable, BaseSource {

    override fun getTag(): String {
        return bookSourceName
    }

    override fun getKey(): String {
        return bookSourceUrl
    }

    override fun getSource(): BaseSource {
        return this
    }

    @delegate:Transient
    @delegate:Ignore
    @IgnoredOnParcel
    val exploreKinds: List<ExploreKind> by lazy {
        val exploreUrl = exploreUrl ?: return@lazy emptyList()
        val kinds = arrayListOf<ExploreKind>()
        var ruleStr = exploreUrl
        if (ruleStr.isNotBlank()) {
            kotlin.runCatching {
                if (exploreUrl.startsWith("<js>", false)
                    || exploreUrl.startsWith("@js:", false)
                ) {
                    val aCache = ACache.get(appCtx, "explore")
                    ruleStr = aCache.getAsString(bookSourceUrl) ?: ""
                    if (ruleStr.isBlank()) {
                        val jsStr = if (exploreUrl.startsWith("@")) {
                            exploreUrl.substring(4)
                        } else {
                            exploreUrl.substring(4, exploreUrl.lastIndexOf("<"))
                        }
                        ruleStr = evalJS(jsStr).toString().trim()
                        aCache.put(bookSourceUrl, ruleStr)
                    }
                }
                if (ruleStr.isJsonArray()) {
                    GSON.fromJsonArray<ExploreKind>(ruleStr)?.let {
                        kinds.addAll(it)
                    }
                } else {
                    ruleStr.split("(&&|\n)+".toRegex()).forEach { kindStr ->
                        val kindCfg = kindStr.split("::")
                        kinds.add(ExploreKind(kindCfg.first(), kindCfg.getOrNull(1)))
                    }
                }
            }.onFailure {
                kinds.add(ExploreKind("ERROR:${it.localizedMessage}", it.stackTraceToString()))
                it.printOnDebug()
            }
        }
        return@lazy kinds
    }

    override fun hashCode(): Int {
        return bookSourceUrl.hashCode()
    }

    override fun equals(other: Any?) =
        if (other is BookSource) other.bookSourceUrl == bookSourceUrl else false

    fun getLoginUiStr(): String? {
        return loginUi?.let {
            GSON.toJson(it)
        }
    }

    fun getSearchRule() = ruleSearch ?: SearchRule()

    fun getExploreRule() = ruleExplore ?: ExploreRule()

    fun getBookInfoRule() = ruleBookInfo ?: BookInfoRule()

    fun getTocRule() = ruleToc ?: TocRule()

    fun getContentRule() = ruleContent ?: ContentRule()

    fun addGroup(group: String) {
        bookSourceGroup?.let {
            if (!it.contains(group)) {
                bookSourceGroup = "$it,$group"
            }
        } ?: let {
            bookSourceGroup = group
        }
    }

    fun removeGroup(group: String) {
        bookSourceGroup?.splitNotBlank("[,;，；]".toRegex())?.toHashSet()?.let {
            it.remove(group)
            bookSourceGroup = TextUtils.join(",", it)
        }
    }

    fun equal(source: BookSource) =
        equal(bookSourceName, source.bookSourceName)
                && equal(bookSourceUrl, source.bookSourceUrl)
                && equal(bookSourceGroup, source.bookSourceGroup)
                && bookSourceType == source.bookSourceType
                && equal(bookUrlPattern, source.bookUrlPattern)
                && equal(bookSourceComment, source.bookSourceComment)
                && enabled == source.enabled
                && enabledExplore == source.enabledExplore
                && equal(header, source.header)
                && loginUrl == source.loginUrl
                && equal(exploreUrl, source.exploreUrl)
                && equal(searchUrl, source.searchUrl)
                && getSearchRule() == source.getSearchRule()
                && getExploreRule() == source.getExploreRule()
                && getBookInfoRule() == source.getBookInfoRule()
                && getTocRule() == source.getTocRule()
                && getContentRule() == source.getContentRule()

    private fun equal(a: String?, b: String?) = a == b || (a.isNullOrEmpty() && b.isNullOrEmpty())

    class Converters {
        @TypeConverter
        fun loginUiRuleToString(loginUi: List<RowUi>?): String = GSON.toJson(loginUi)

        @TypeConverter
        fun stringToLoginRule(json: String?): List<RowUi>? = GSON.fromJsonArray(json)

        @TypeConverter
        fun exploreRuleToString(exploreRule: ExploreRule?): String = GSON.toJson(exploreRule)

        @TypeConverter
        fun stringToExploreRule(json: String?) = GSON.fromJsonObject<ExploreRule>(json)

        @TypeConverter
        fun searchRuleToString(searchRule: SearchRule?): String = GSON.toJson(searchRule)

        @TypeConverter
        fun stringToSearchRule(json: String?) = GSON.fromJsonObject<SearchRule>(json)

        @TypeConverter
        fun bookInfoRuleToString(bookInfoRule: BookInfoRule?): String = GSON.toJson(bookInfoRule)

        @TypeConverter
        fun stringToBookInfoRule(json: String?) = GSON.fromJsonObject<BookInfoRule>(json)

        @TypeConverter
        fun tocRuleToString(tocRule: TocRule?): String = GSON.toJson(tocRule)

        @TypeConverter
        fun stringToTocRule(json: String?) = GSON.fromJsonObject<TocRule>(json)

        @TypeConverter
        fun contentRuleToString(contentRule: ContentRule?): String = GSON.toJson(contentRule)

        @TypeConverter
        fun stringToContentRule(json: String?) = GSON.fromJsonObject<ContentRule>(json)

    }
}