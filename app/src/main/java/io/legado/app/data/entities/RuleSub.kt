package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ruleSubs")
data class RuleSub(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    var name: String = "",
    var url: String = "",
    var type: Int = 0,
    var customOrder: Int = 0,
    var autoUpdate: Boolean = false,
    var update: Long = System.currentTimeMillis()
)
