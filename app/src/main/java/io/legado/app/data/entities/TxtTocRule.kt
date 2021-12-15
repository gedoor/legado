package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey


@Entity(tableName = "txtTocRules")
data class TxtTocRule(
    @PrimaryKey
    var id: Long = System.currentTimeMillis(),
    var name: String = "",
    var rule: String = "",
    var serialNumber: Int = -1,
    var enable: Boolean = true
) {

    @Ignore
    constructor() : this(name = "")

}