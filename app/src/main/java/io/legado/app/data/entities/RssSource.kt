package io.legado.app.data.entities

import androidx.room.Entity

@Entity(tableName = "rssSources")
data class RssSource(
    var sourceName: String
)