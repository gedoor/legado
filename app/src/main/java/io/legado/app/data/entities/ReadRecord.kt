package io.legado.app.data.entities

import androidx.room.Entity

@Entity(tableName = "readRecord", primaryKeys = ["androidId", "bookName"])
data class ReadRecord(
    var androidId: String = "",
    var bookName: String = "",
    var readTime: Long = 0L
)