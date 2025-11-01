package io.legado.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.parcelize.IgnoredOnParcel

@Entity(
    tableName = "rssArticles",
    primaryKeys = ["origin", "link", "sort"]
)
data class RssArticle(
    override var origin: String = "",
    var sort: String = "",
    var title: String = "",
    var order: Long = 0,
    override var link: String = "",
    var pubDate: String? = null,
    var description: String? = null,
    var content: String? = null,
    var image: String? = null,
    @ColumnInfo(defaultValue = "默认分组")
    var group: String = "默认分组",
    var read: Boolean = false,
    override var variable: String? = null,
    /**类型 0网页，1图片，2视频**/
    @ColumnInfo(defaultValue = "0")
    var type: Int = 0,
    /**阅读进度**/
    @ColumnInfo(defaultValue = "0")
    var durPos: Int = 0
) : BaseRssArticle {

    override fun hashCode() = link.hashCode()

    override fun equals(other: Any?): Boolean {
        other ?: return false
        return if (other is RssArticle) origin == other.origin && link == other.link && sort == other.sort else false
    }

    @delegate:Transient
    @delegate:Ignore
    @IgnoredOnParcel
    override val variableMap: HashMap<String, String> by lazy {
        GSON.fromJsonObject<HashMap<String, String>>(variable).getOrNull() ?: hashMapOf()
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
        image = image,
        group = group,
        variable = variable,
        type = type,
        durPos = durPos
    )
}
