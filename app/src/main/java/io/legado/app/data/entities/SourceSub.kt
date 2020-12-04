package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sourceSubs")
data class SourceSub(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val url: String,
)
