package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jayway.jsonpath.DocumentContext
import io.legado.app.utils.*
import kotlinx.parcelize.Parcelize
import splitties.init.appCtx

@Parcelize
@Entity(tableName = "rssSources", indices = [(Index(value = ["sourceUrl"], unique = false))])
data class RssSource(
    @PrimaryKey
    var sourceUrl: String = "",
    var sourceName: String = "",
    var sourceIcon: String = "",
    var sourceGroup: String? = null,
    var sourceComment: String? = null,
    var enabled: Boolean = true,
    override var concurrentRate: String? = null,    //并发率
    override var header: String? = null,            // 请求头
    override var loginUrl: String? = null,          // 登录地址
    override var loginUi: String? = null,               //登录UI
    var loginCheckJs: String? = null,               //登录检测js
    var sortUrl: String? = null,
    var singleUrl: Boolean = false,
    //列表规则
    var articleStyle: Int = 0,                      //列表样式,0,1,2
    var ruleArticles: String? = null,
    var ruleNextPage: String? = null,
    var ruleTitle: String? = null,
    var rulePubDate: String? = null,
    //webView规则
    var ruleDescription: String? = null,
    var ruleImage: String? = null,
    var ruleLink: String? = null,
    var ruleContent: String? = null,
    var style: String? = null,
    var enableJs: Boolean = true,
    var loadWithBaseUrl: Boolean = true,
    var customOrder: Int = 0
) : Parcelable, BaseSource {

    @Ignore
    constructor() : this("")

    override fun getTag(): String {
        return sourceName
    }

    override fun getKey(): String {
        return sourceUrl
    }

    override fun getSource(): BaseSource {
        return this
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
                && equal(ruleArticles, source.ruleArticles)
                && equal(ruleNextPage, source.ruleNextPage)
                && equal(ruleTitle, source.ruleTitle)
                && equal(rulePubDate, source.rulePubDate)
                && equal(ruleDescription, source.ruleDescription)
                && equal(ruleLink, source.ruleLink)
                && equal(ruleContent, source.ruleContent)
                && enableJs == source.enableJs
                && loadWithBaseUrl == source.loadWithBaseUrl
    }

    private fun equal(a: String?, b: String?): Boolean {
        return a == b || (a.isNullOrEmpty() && b.isNullOrEmpty())
    }

    fun sortUrls(): List<Pair<String, String>> = arrayListOf<Pair<String, String>>().apply {
        kotlin.runCatching {
            var a = sortUrl
            if (sortUrl?.startsWith("<js>", false) == true
                || sortUrl?.startsWith("@js:", false) == true
            ) {
                val aCache = ACache.get(appCtx, "rssSortUrl")
                a = aCache.getAsString(sourceUrl) ?: ""
                if (a.isBlank()) {
                    val jsStr = if (sortUrl!!.startsWith("@")) {
                        sortUrl!!.substring(4)
                    } else {
                        sortUrl!!.substring(4, sortUrl!!.lastIndexOf("<"))
                    }
                    a = evalJS(jsStr).toString()
                    aCache.put(sourceUrl, a)
                }
            }
            a?.split("(&&|\n)+".toRegex())?.forEach { c ->
                val d = c.split("::")
                if (d.size > 1)
                    add(Pair(d[0], d[1]))
            }
            if (isEmpty()) {
                add(Pair("", sourceUrl))
            }
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {

        fun fromJsonDoc(doc: DocumentContext): RssSource? {
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
                    enableJs = doc.readBool("$.enableJs") ?: true,
                    loadWithBaseUrl = doc.readBool("$.loadWithBaseUrl") ?: true,
                    customOrder = doc.readInt("$.customOrder") ?: 0
                )
            }.getOrNull()
        }

        fun fromJson(json: String): RssSource? {
            return fromJsonDoc(jsonPath.parse(json))
        }

        fun fromJsonArray(jsonArray: String): ArrayList<RssSource> {
            val sources = arrayListOf<RssSource>()
            val doc = jsonPath.parse(jsonArray).read<List<*>>("$")
            doc.forEach {
                val jsonItem = jsonPath.parse(it)
                fromJsonDoc(jsonItem)?.let { source ->
                    sources.add(source)
                }
            }
            return sources
        }
    }

}