package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize


@Parcelize
@Entity(tableName = "chapters",
    indices = [(Index(value = ["url"]))])
data class Chapter(@PrimaryKey
                    var url: String = "",
                    var name: String = "",
                    var bookUrl: String = "",
                    var index: Int = 0,
                    var ResourceUrl: String? = null,
                    var tag: String? = null,
                    var start: Long? = null,
                    var end: Long? = null
) : Parcelable

