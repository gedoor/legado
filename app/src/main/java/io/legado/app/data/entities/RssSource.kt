package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "rssSources")
data class RssSource(
    var sourceName: String,
    var enabled: Boolean = true
) : Parcelable