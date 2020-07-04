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

    override fun hashCode(): Int {
        return link.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (other is RssArticle) {
            return origin == other.origin && link == other.link
        }
        return false
    }

    fun toStar(): RssStar {
        return RssStar(
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
}