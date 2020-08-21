package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ReadRecord(
    @PrimaryKey
    val bookName: String = "",
    val readTime: Long = 0L
)