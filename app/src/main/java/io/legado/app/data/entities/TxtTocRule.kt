package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "txtTocRules")
data class TxtTocRule(
    @PrimaryKey
    var name: String = "",
    var rule: String = "",
    var serialNumber: Int = -1,
    var enable: Boolean = true
)