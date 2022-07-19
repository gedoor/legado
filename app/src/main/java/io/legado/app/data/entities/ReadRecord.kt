package io.legado.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "readRecord", primaryKeys = ["deviceId", "bookName"])
data class ReadRecord(
    var deviceId: String = "",
    var bookName: String = "",
    @ColumnInfo(defaultValue = "0")
    var readTime: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    var lastRead: Long = System.currentTimeMillis()
)