package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.legado.app.utils.GSON


@Entity(tableName = "txtTocRules")
data class TxtTocRule(
    @PrimaryKey
    var id: Long = System.currentTimeMillis(),
    var name: String = "",
    var rule: String = "",
    var example: String? = null,
    var serialNumber: Int = -1,
    var enable: Boolean = true
) {

    override fun hashCode(): Int {
        return GSON.toJson(this).hashCode()
    }

    override fun equals(other: Any?): Boolean {
        other ?: return false
        if (other is TxtTocRule) {
            return id == other.id
                    && name == other.name
                    && rule == other.rule
                    && example == other.example
                    && serialNumber == other.serialNumber
                    && enable == other.enable
        }
        return false
    }

}