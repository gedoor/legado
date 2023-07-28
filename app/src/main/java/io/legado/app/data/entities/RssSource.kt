package io.legado.app.data.entities

import android.os.Parcelable
import android.text.TextUtils
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.legado.app.constant.AppPattern
import io.legado.app.utils.splitNotBlank
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
    // js库
    override var jsLib: String? = null,
    // 启用okhttp CookieJAr 自动保存每次请求的cookie
    @ColumnInfo(defaultValue = "0")
    override var enabledCookieJar: Boolean? = true,
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
    /**
     * 跳转url拦截,
     * js, 返回true拦截,js变量url,可以通过js打开url,比如调用阅读搜索,添加书架等,简化规则写法,不用webView js注入
     * **/
    var shouldOverrideUrlLoading: String? = null,
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
                && equal(sourceName, source.sourceName)
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

}
