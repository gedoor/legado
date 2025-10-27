package io.legado.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rssReadRecords")
data class RssReadRecord(
    @PrimaryKey
    val record: String,
    val title: String? = null,
    val readTime: Long? = null,
    val read: Boolean = true,
    @ColumnInfo(defaultValue = "")
    val origin: String = "",
    @ColumnInfo(defaultValue = "")
    var sort: String = "",
    var image: String? = null,
    /**类型 0网页，1图片，2视频**/
    @ColumnInfo(defaultValue = "0")
    var type: Int = 0,
    /**阅读进度**/
    @ColumnInfo(defaultValue = "0")
    var durPos: Int = 0
)