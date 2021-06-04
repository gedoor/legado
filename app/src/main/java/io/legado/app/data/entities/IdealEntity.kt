package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.legado.app.R
import kotlinx.parcelize.Parcelize

/**
 * 想法
 */
@Parcelize
@Entity(tableName = "book_ideals")
data class IdealEntity(

    @PrimaryKey
    var idealId: Long = 0,

    // 在本章里面的第几个想法
    var inChapterIndex: Int = 0,

    // 书id
    var bookName: String = "",

    // 第几章的想法
    var chapterIndex: Int = 0,

    // 章节的第几个字符开始
    var startIndex: Int = 0,

    // 本章的第几个字符结束
    var endIndex: Int = 0

) : Parcelable {


    override fun hashCode(): Int {
        return idealId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is IdealEntity) {
            return idealId == other.idealId &&
                    chapterIndex == other.chapterIndex &&
                    startIndex == other.startIndex &&
                    endIndex == other.endIndex
        }

        return false
    }

    fun getLineColor() = R.color.md_red_300
}