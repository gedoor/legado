package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cookies")
data class Cookie(
    @PrimaryKey
    var url: String,
    var cookie: String
)