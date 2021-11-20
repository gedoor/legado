package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.Ignore

@Entity(tableName = "readRecord", primaryKeys = ["deviceId", "bookName"])
data class ReadRecord(
    var deviceId: String = "",
    var bookName: String = "",
    var readTime: Long = 0L
) {

    @Ignore
    constructor() : this(deviceId = "")

}