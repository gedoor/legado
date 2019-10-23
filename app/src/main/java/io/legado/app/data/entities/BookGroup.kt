package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "book_groups")
data class BookGroup(
        @PrimaryKey
        var groupId: Int = 0,
        var groupName: String,
        var order: Int = 0
) : Parcelable