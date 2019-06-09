package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_groups")
data class BookGroup(
        @PrimaryKey
        var groupId: Int = 0,
        var groupName: String,
        var order: Int = 0
)