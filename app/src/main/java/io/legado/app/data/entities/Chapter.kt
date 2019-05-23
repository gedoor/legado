package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize


@Parcelize
@Entity(tableName = "chapters",
    indices = [(Index(value = ["feedId", "feedLink"], unique = true))])
data class Chapter(@PrimaryKey
                    var name: String = "",
                    var bookUrl: String = "",
                    var index: Int = 0,
                    var resourceUrl: String? = null,
                    var tag: String? = null,
                    var start: Long? = null,
                    var end: Long? = null
) : Parcelable

