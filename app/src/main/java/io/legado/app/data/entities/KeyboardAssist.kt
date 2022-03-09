package io.legado.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "keyboardAssists", primaryKeys = ["type", "key"])
data class KeyboardAssist(
    @ColumnInfo(defaultValue = "0")
    val type: Int = 0,
    @ColumnInfo(defaultValue = "")
    val key: String,
    @ColumnInfo(defaultValue = "")
    val value: String,
    @ColumnInfo(defaultValue = "0")
    val serialNo: Int
)