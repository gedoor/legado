package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey


@Entity(tableName = "rssArticles")
data class RssArticle(
    var origin: String = "",
    var time: Long = System.currentTimeMillis(),
    @PrimaryKey
    var guid: String = "",
    var title: String? = null,
    var author: String? = null,
    var link: String? = null,
    var pubDate: String? = null,
    var description: String? = null,
    var content: String? = null,
    var image: String? = null,
    var categories: String? = null
) {

    @Ignore
    var categoryList: MutableList<String> = mutableListOf()

}