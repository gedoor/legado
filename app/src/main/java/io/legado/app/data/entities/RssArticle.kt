package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.Ignore


@Entity(
    tableName = "rssArticles",
    primaryKeys = ["origin", "link"]
)
data class RssArticle(
    var origin: String = "",
    var title: String = "",
    var order: Long = 0,
    var link: String = "",
    var pubDate: String? = null,
    var description: String? = null,
    var content: String? = null,
    var image: String? = null,
    var categories: String? = null,
    var read: Boolean = false,
    var star: Boolean = false
) {

    @Ignore
    var categoryList: MutableList<String> = mutableListOf()

}