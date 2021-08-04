package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.Ignore
import io.legado.app.model.analyzeRule.RuleDataInterface
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.parcelize.IgnoredOnParcel


@Entity(
    tableName = "rssArticles",
    primaryKeys = ["origin", "link"]
)
data class RssArticle(
    var origin: String = "",
    var sort: String = "",
    var title: String = "",
    var order: Long = 0,
    var link: String = "",
    var pubDate: String? = null,
    var description: String? = null,
    var content: String? = null,
    var image: String? = null,
    var read: Boolean = false,
    var variable: String? = null
) : RuleDataInterface {

    override fun hashCode() = link.hashCode()

    override fun equals(other: Any?): Boolean {
        other ?: return false
        return if (other is RssArticle) origin == other.origin && link == other.link else false
    }

    @delegate:Transient
    @delegate:Ignore
    @IgnoredOnParcel
    override val variableMap by lazy {
        GSON.fromJsonObject<HashMap<String, String>>(variable) ?: HashMap()
    }

    override fun putVariable(key: String, value: String) {
        variableMap[key] = value
        variable = GSON.toJson(variableMap)
    }

    fun toStar() = RssStar(
        origin = origin,
        sort = sort,
        title = title,
        starTime = System.currentTimeMillis(),
        link = link,
        pubDate = pubDate,
        description = description,
        content = content,
        image = image
    )
}