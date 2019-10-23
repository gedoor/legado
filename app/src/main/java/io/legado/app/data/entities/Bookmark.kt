package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "bookmarks", indices = [(Index(value = ["bookUrl"], unique = true))])
data class Bookmark(
    @PrimaryKey
    var time: Long = System.currentTimeMillis(),
    var bookUrl: String = "",
    var bookName: String = "",
    var chapterName: String = "",
    var chapterIndex: Int = 0,
    var pageIndex: Int = 0,
    var content: String = ""

) : Parcelable