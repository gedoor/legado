package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "readRecord")
data class ReadRecord(
    @PrimaryKey
    var bookName: String = "",
    var readTime: Long = 0L
)