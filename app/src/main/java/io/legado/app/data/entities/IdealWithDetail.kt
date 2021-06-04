package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import kotlinx.parcelize.Parcelize

@Parcelize
data class IdealWithDetail(

    @Embedded
    var idealEntity: IdealEntity,

    @Relation(
        parentColumn = "idealId",
        entityColumn = "id",
        entity = IdealDetailEntity::class,
        associateBy = Junction(IdealRelation::class)
    )
    var idealList: List<IdealDetailEntity>
) : Parcelable