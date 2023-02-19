package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 字典规则
 */
@Entity(tableName = "dictRules")
data class DictRule(
    @PrimaryKey
    val name: String,
    var urlRule: String,
    var showRule: String,
    var enabled: Boolean
)