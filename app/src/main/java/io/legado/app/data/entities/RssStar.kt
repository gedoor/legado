package io.legado.app.data.entities

import androidx.room.Entity


@Entity(
    tableName = "rssStars",
    primaryKeys = ["origin", "link"]
)
data class RssStar(
    var origin: String = "",
    var sort: String = "",
    var title: String = "",
    var starTime: Long = 0,
    var link: String = "",
    var pubDate: String? = null,
    var description: String? = null,
    var content: String? = null,
    var image: String? = null
) {
    fun toRssArticle() = RssArticle(
            origin = origin,
            sort = sort,
            title = title,
            link = link,
            pubDate = pubDate,
            description = description,
            content = content,
            image = image
        )
}