package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "rssSources")
data class RssSource(
    var sourceName: String,
    @PrimaryKey
    var sourceUrl: String,
    var enabled: Boolean = true
) : Parcelable