package io.legado.app.data.entities

import android.os.Parcelable
import android.text.TextUtils
import androidx.room.*
import com.jayway.jsonpath.DocumentContext
import io.legado.app.constant.AppPattern
import io.legado.app.utils.*
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "rssSources", indices = [(Index(value = ["sourceUrl"], unique = false))])
data class RssSource(
    @PrimaryKey
    var sourceUrl: String = "",
    // 名称
    var sourceName: String = "",
    // 图标
    var sourceIcon: String = "",
    // 分组
    var sourceGroup: String? = null,
    // 注释
    var sourceComment: String? = null,
    // 是否启用
    var enabled: Boolean = true,
    // 自定义变量说明
    var variableComment: String? = null,
    @ColumnInfo(defaultValue = "0")
    override var enabledCookieJar: Boolean? = false,
    /**并发率**/
    override var concurrentRate: String? = null,
    /**请求头**/
    override var header: String? = null,
    /**登录地址**/
    override var loginUrl: String? = null,
    /**登录Ui**/
    override var loginUi: String? = null,
    /**登录检测js**/
    var loginCheckJs: String? = null,
    /**封面解密js**/
    var coverDecodeJs: String? = null,
    /**分类Url**/
    var sortUrl: String? = null,
    /**是否单url源**/
    var singleUrl: Boolean = false,
    /*列表规则*/
    /**列表样式,0,1,2**/
    @ColumnInfo(defaultValue = "0")
    var articleStyle: Int = 0,
    /**列表规则**/
    var ruleArticles: String? = null,
    /**下一页规则**/
    var ruleNextPage: String? = null,
    /**标题规则**/
    var ruleTitle: String? = null,
    /**发布日期规则**/
    var rulePubDate: String? = null,
    /*webView规则*/
    /**描述规则**/
    var ruleDescription: String? = null,
    /**图片规则**/
    var ruleImage: String? = null,
    /**链接规则**/
    var ruleLink: String? = null,
    /**正文规则**/
    var ruleContent: String? = null,
    /**正文url白名单**/
    var contentWhitelist: String? = null,
    /**正文url黑名单**/
    var contentBlacklist: String? = null,
    /**webView样式**/
    var style: String? = null,
    @ColumnInfo(defaultValue = "1")
    var enableJs: Boolean = true,
    @ColumnInfo(defaultValue = "1")
    var loadWithBaseUrl: Boolean = true,
    /**注入js**/
    var injectJs: String? = null,
    /*其它规则*/
    /**最后更新时间，用于排序**/
    @ColumnInfo(defaultValue = "0")
    var lastUpdateTime: Long = 0,
    @ColumnInfo(defaultValue = "0")
    var customOrder: Int = 0
) : Parcelable, BaseSource {

    override fun getTag(): String {
        return sourceName
    }

    override fun getKey(): String {
        return sourceUrl
    }

    override fun equals(other: Any?): Boolean {
        if (other is RssSource) {
            return other.sourceUrl == sourceUrl
        }
        return false
    }

    override fun hashCode() = sourceUrl.hashCode()

    fun equal(source: RssSource): Boolean {
        return equal(sourceUrl, source.sourceUrl)
                && equal(sourceIcon, source.sourceIcon)
                && enabled == source.enabled
                && equal(sourceGroup, source.sourceGroup)
                && enabledCookieJar == source.enabledCookieJar
                && equal(sourceComment, source.sourceComment)
                && equal(concurrentRate, source.concurrentRate)
                && equal(header, source.header)
                && equal(loginUrl, source.loginUrl)
                && equal(loginUi, source.loginUi)
                && equal(loginCheckJs, source.loginCheckJs)
                && equal(coverDecodeJs, source.coverDecodeJs)
                && equal(sortUrl, source.sortUrl)
                && singleUrl == source.singleUrl
                && articleStyle == source.articleStyle
                && equal(ruleArticles, source.ruleArticles)
                && equal(ruleNextPage, source.ruleNextPage)
                && equal(ruleTitle, source.ruleTitle)
                && equal(rulePubDate, source.rulePubDate)
                && equal(ruleDescription, source.ruleDescription)
                && equal(ruleLink, source.ruleLink)
                && equal(ruleContent, source.ruleContent)
                && enableJs == source.enableJs
                && loadWithBaseUrl == source.loadWithBaseUrl
                && equal(variableComment, source.variableComment)
                && equal(style, source.style)
                && equal(injectJs, source.injectJs)
    }

    private fun equal(a: String?, b: String?): Boolean {
        return a == b || (a.isNullOrEmpty() && b.isNullOrEmpty())
    }

    fun getDisplayNameGroup(): String {
        return if (sourceGroup.isNullOrBlank()) {
            sourceName
        } else {
            String.format("%s (%s)", sourceName, sourceGroup)
        }
    }

    fun addGroup(groups: String): RssSource {
        sourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.toHashSet()?.let {
            it.addAll(groups.splitNotBlank(AppPattern.splitGroupRegex))
            sourceGroup = TextUtils.join(",", it)
        }
        if (sourceGroup.isNullOrBlank()) sourceGroup = groups
        return this
    }

    fun removeGroup(groups: String): RssSource {
        sourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.toHashSet()?.let {
            it.removeAll(groups.splitNotBlank(AppPattern.splitGroupRegex).toSet())
            sourceGroup = TextUtils.join(",", it)
        }
        return this
    }

    fun getDisplayVariableComment(otherComment: String): String {
        return if (variableComment.isNullOrBlank()) {
            otherComment
        } else {
            "${variableComment}\n$otherComment"
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {

        fun fromJsonDoc(doc: DocumentContext): Result<RssSource> {
            return kotlin.runCatching {
                val loginUi = doc.read<Any>("$.loginUi")
                RssSource(
                    sourceUrl = doc.readString("$.sourceUrl")!!,
                    sourceName = doc.readString("$.sourceName")!!,
                    sourceIcon = doc.readString("$.sourceIcon") ?: "",
                    sourceGroup = doc.readString("$.sourceGroup"),
                    sourceComment = doc.readString("$.sourceComment"),
                    enabled = doc.readBool("$.enabled") ?: true,
                    concurrentRate = doc.readString("$.concurrentRate"),
                    header = doc.readString("$.header"),
                    loginUrl = doc.readString("$.loginUrl"),
                    loginUi = if (loginUi is List<*>) GSON.toJson(loginUi) else loginUi?.toString(),
                    loginCheckJs = doc.readString("$.loginCheckJs"),
                    sortUrl = doc.readString("$.sortUrl"),
                    singleUrl = doc.readBool("$.singleUrl") ?: false,
                    articleStyle = doc.readInt("$.articleStyle") ?: 0,
                    ruleArticles = doc.readString("$.ruleArticles"),
                    ruleNextPage = doc.readString("$.ruleNextPage"),
                    ruleTitle = doc.readString("$.ruleTitle"),
                    rulePubDate = doc.readString("$.rulePubDate"),
                    ruleDescription = doc.readString("$.ruleDescription"),
                    ruleImage = doc.readString("$.ruleImage"),
                    ruleLink = doc.readString("$.ruleLink"),
                    ruleContent = doc.readString("$.ruleContent"),
                    style = doc.readString("$.style"),
                    injectJs = doc.readString("$.injectJs"),
                    enableJs = doc.readBool("$.enableJs") ?: true,
                    loadWithBaseUrl = doc.readBool("$.loadWithBaseUrl") ?: true,
                    enabledCookieJar = doc.readBool("$.enabledCookieJar") ?: false,
                    customOrder = doc.readInt("$.customOrder") ?: 0,
                    lastUpdateTime = doc.readLong("$.lastUpdateTime") ?: 0L,
                    coverDecodeJs = doc.readString("$.coverDecodeJs"),
                    variableComment = doc.readString("$.variableComment")
                )
            }
        }

        fun fromJson(json: String): Result<RssSource> {
            return fromJsonDoc(jsonPath.parse(json))
        }

        fun fromJsonArray(jsonArray: String): Result<ArrayList<RssSource>> {
            return kotlin.runCatching {
                val sources = arrayListOf<RssSource>()
                val doc = jsonPath.parse(jsonArray).read<List<*>>("$")
                doc.forEach {
                    val jsonItem = jsonPath.parse(it)
                    fromJsonDoc(jsonItem).getOrThrow().let { source ->
                        sources.add(source)
                    }
                }
                sources
            }
        }
    }

}
