package io.legado.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "keyboardAssists", primaryKeys = ["type", "key"])
data class KeyboardAssist(
    @ColumnInfo(defaultValue = "0")
    var type: Int = 0,
    @ColumnInfo(defaultValue = "")
    var key: String,
    @ColumnInfo(defaultValue = "")
    var value: String,
    @ColumnInfo(defaultValue = "0")
    var serialNo: Int = 0
)