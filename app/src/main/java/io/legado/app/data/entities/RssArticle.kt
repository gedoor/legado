package io.legado.app.data.entities

import androidx.room.Entity


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
    var read: Boolean = false
) {
    
    override fun hashCode() = link.hashCode()
    
    override fun equals(other: Any?): Boolean {
        other ?: return false
        return if (other is RssArticle) origin == other.origin && link == other.link else false
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