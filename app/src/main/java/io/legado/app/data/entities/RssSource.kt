package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.legado.app.utils.ACache
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
    var sortUrl: String? = null,
    var singleUrl: Boolean = false,
    var articleStyle: Int = 0,
    //列表规则
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
    override var header: String? = null,
    var enableJs: Boolean = false,
    var loadWithBaseUrl: Boolean = false,

    var customOrder: Int = 0
) : Parcelable, BaseSource {

    override fun getStoreUrl(): String {
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
}