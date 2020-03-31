package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import io.legado.app.App
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppConst.userAgent
import io.legado.app.constant.BookType
import io.legado.app.data.entities.rule.*
import io.legado.app.help.JsExtensions
import io.legado.app.utils.ACache
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.getPrefString
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.util.*
import javax.script.SimpleBindings

@Parcelize
@Entity(
    tableName = "book_sources",
    indices = [(Index(value = ["bookSourceUrl"], unique = false))]
)
data class BookSource(
    var bookSourceName: String = "",                // 名称
    var bookSourceGroup: String? = null,            // 分组
    @PrimaryKey
    var bookSourceUrl: String = "",                 // 地址，包括 http/https
    var bookSourceType: Int = BookType.default,     // 类型，0 文本，1 音频
    var bookUrlPattern: String? = null,             // 详情页url正则
    var customOrder: Int = 0,                       // 手动排序编号
    var enabled: Boolean = true,                    // 是否启用
    var enabledExplore: Boolean = true,             // 启用发现
    var header: String? = null,                     // 请求头
    var loginUrl: String? = null,                   // 登录地址
    var lastUpdateTime: Long = 0,                   // 最后更新时间，用于排序
    var weight: Int = 0,                            // 智能排序的权重
    var exploreUrl: String? = null,                 // 发现url
    var ruleExplore: String? = null,                // 发现规则
    var searchUrl: String? = null,                  // 搜索url
    var ruleSearch: String? = null,                 // 搜索规则
    var ruleBookInfo: String? = null,               // 书籍信息页规则
    var ruleToc: String? = null,                    // 目录页规则
    var ruleContent: String? = null                 // 正文页规则
) : Parcelable, JsExtensions {

    override fun hashCode(): Int {
        return bookSourceUrl.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is BookSource) {
            return other.bookSourceUrl == bookSourceUrl
        }
        return false
    }

    @Ignore
    @IgnoredOnParcel
    private var searchRuleV: SearchRule? = null

    @Ignore
    @IgnoredOnParcel
    private var exploreRuleV: ExploreRule? = null

    @Ignore
    @IgnoredOnParcel
    private var bookInfoRuleV: BookInfoRule? = null

    @Ignore
    @IgnoredOnParcel
    private var tocRuleV: TocRule? = null

    @Ignore
    @IgnoredOnParcel
    private var contentRuleV: ContentRule? = null

    @Throws(Exception::class)
    fun getHeaderMap(): Map<String, String> {
        val headerMap = HashMap<String, String>()
        headerMap[AppConst.UA_NAME] = App.INSTANCE.getPrefString("user_agent") ?: userAgent
        header?.let {
            val header1 = when {
                it.startsWith("@js:", true) ->
                    evalJS(it.substring(4)).toString()
                it.startsWith("<js>", true) ->
                    evalJS(it.substring(4, it.lastIndexOf("<"))).toString()
                else -> it
            }
            GSON.fromJsonObject<Map<String, String>>(header1)?.let { map ->
                headerMap.putAll(map)
            }
        }
        return headerMap
    }

    fun getSearchRule(): SearchRule {
        searchRuleV ?: let {
            searchRuleV = GSON.fromJsonObject<SearchRule>(ruleSearch)
            searchRuleV ?: let { searchRuleV = SearchRule() }
        }
        return searchRuleV!!
    }

    fun getExploreRule(): ExploreRule {
        exploreRuleV ?: let {
            exploreRuleV = GSON.fromJsonObject<ExploreRule>(ruleExplore)
            exploreRuleV ?: let { exploreRuleV = ExploreRule() }
        }
        return exploreRuleV!!
    }

    fun getBookInfoRule(): BookInfoRule {
        bookInfoRuleV ?: let {
            bookInfoRuleV = GSON.fromJsonObject<BookInfoRule>(ruleBookInfo)
            bookInfoRuleV ?: let { bookInfoRuleV = BookInfoRule() }
        }
        return bookInfoRuleV!!
    }

    fun getTocRule(): TocRule {
        tocRuleV ?: let {
            tocRuleV = GSON.fromJsonObject<TocRule>(ruleToc)
            tocRuleV ?: let { tocRuleV = TocRule() }
        }
        return tocRuleV!!
    }

    fun getContentRule(): ContentRule {
        contentRuleV ?: let {
            contentRuleV = GSON.fromJsonObject<ContentRule>(ruleContent)
            contentRuleV ?: let { contentRuleV = ContentRule() }
        }
        return contentRuleV!!
    }

    fun addGroup(group: String) {
        bookSourceGroup?.let {
            if (!it.contains(group)) {
                bookSourceGroup = "$it;$group"
            }
        } ?: let {
            bookSourceGroup = group
        }
    }

    fun getExploreKinds(): ArrayList<ExploreKind>? {
        val exploreKinds = arrayListOf<ExploreKind>()
        exploreUrl?.let {
            var a = it
            if (a.isNotBlank()) {
                try {
                    if (it.startsWith("<js>", false)) {
                        val aCache = ACache.get(App.INSTANCE, "explore")
                        a = aCache.getAsString(bookSourceUrl) ?: ""
                        if (a.isBlank()) {
                            val bindings = SimpleBindings()
                            bindings["baseUrl"] = bookSourceUrl
                            bindings["java"] = this
                            a = AppConst.SCRIPT_ENGINE.eval(
                                it.substring(4, it.lastIndexOf("<")),
                                bindings
                            ).toString()
                            aCache.put(bookSourceUrl, a)
                        }
                    }
                    val b = a.split("(&&|\n)+".toRegex())
                    b.forEach { c ->
                        val d = c.split("::")
                        if (d.size > 1)
                            exploreKinds.add(ExploreKind(d[0], d[1]))
                    }
                } catch (e: Exception) {
                    exploreKinds.add(ExploreKind(e.localizedMessage ?: ""))
                }
            }
        }
        return exploreKinds
    }

    /**
     * 执行JS
     */
    @Throws(Exception::class)
    private fun evalJS(jsStr: String): Any {
        val bindings = SimpleBindings()
        bindings["java"] = this
        return AppConst.SCRIPT_ENGINE.eval(jsStr, bindings)
    }

    fun equal(source: BookSource): Boolean {
        return equal(bookSourceName, source.bookSourceName)
                && equal(bookSourceUrl, source.bookSourceUrl)
                && equal(bookSourceGroup, source.bookSourceGroup)
                && bookSourceType == source.bookSourceType
                && equal(bookUrlPattern, source.bookUrlPattern)
                && enabled == source.enabled
                && enabledExplore == source.enabledExplore
                && equal(header, source.header)
                && equal(loginUrl, source.loginUrl)
                && equal(exploreUrl, source.exploreUrl)
                && equal(searchUrl, source.searchUrl)
                && getSearchRule() == source.getSearchRule()
                && getExploreRule() == source.getExploreRule()
                && getBookInfoRule() == source.getBookInfoRule()
                && getTocRule() == source.getTocRule()
                && getContentRule() == source.getContentRule()
    }

    private fun equal(a: String?, b: String?): Boolean {
        return a == b || (a.isNullOrEmpty() && b.isNullOrEmpty())
    }

    data class ExploreKind(
        var title: String,
        var url: String? = null
    )
}