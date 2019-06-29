package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.android.parcel.Parcelize


@Parcelize
@Entity(
        tableName = "chapters",
        primaryKeys = ["url", "bookUrl"],
        indices = [(Index(value = ["bookUrl"], unique = true)), (Index(value = ["bookUrl", "index"], unique = true))],
        foreignKeys = [(ForeignKey(
                entity = Book::class,
                parentColumns = ["descUrl"],
                childColumns = ["bookUrl"],
                onDelete = ForeignKey.CASCADE
        ))]
)    // 删除书籍时自动删除章节
data class BookChapter(
        var url: String = "",               // 章节地址
        var title: String = "",              // 章节标题
        var bookUrl: String = "",           // 书籍地址
        var index: Int = 0,                 // 章节序号
        var resourceUrl: String? = null,    // 音频真实URL
        var tag: String? = null,            //
        var start: Long? = null,            // 章节起始位置
        var end: Long? = null               // 章节终止位置
) : Parcelable

