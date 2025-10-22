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
    var image: String? = null
)