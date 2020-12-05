package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sourceSubs")
data class SourceSub(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    var name: String = "",
    var url: String = "",
    var type: Int = 0,
    var customOrder: Int = 0
) {

    fun setType(type: Type) {
        this.type = type.ordinal
    }

    enum class Type {
        BookSource, RssSource
    }
}
