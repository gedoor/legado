package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize

@Parcelize
class BookChapterReview(
    @ColumnInfo(defaultValue = "0")
    var bookId: Long = 0,
    var chapterId: Long = 0,
    var summaryUrl: String = "",
): Parcelable {

}
