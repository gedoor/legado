package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = "ideal_detail_list",
    primaryKeys = ["id", "idealId"]
)
@Parcelize
data class IdealRelation(
    var id: Long = 0b1,
    var idealId: Long = 0b1
) : Parcelable