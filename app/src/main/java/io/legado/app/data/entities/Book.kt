package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.*
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
@Entity(tableName = "books",
    indices = [(Index(value = ["url"]))])
data class Book(@PrimaryKey
                var url: String = "",
                var name: String = "",
                var tag: String = "",
                var author: String? = null,
                var coverUrl: String? = null,
                var customCoverUrl: String? = null,
                var introduction: String? = null,
                var charset: String? = null,
                var type: Int = 0,   // 0: text, 1: audio
                var latestChapterName: String? = null,
                var lastUpdateTime: Date? = null,
                var latestChapterTime: Date? = null,
                var durChapterIndex: Int = 0,
                var durChapterPage: Int = 0,
                var totalChapterNum: Int = 0,
                var hasNewChapter: Boolean = false,
                var allowUpdate: Boolean = true
                ) : Parcelable {

    fun getUnreadChapterNum() = Math.max(totalChapterNum - durChapterIndex - 1, 0)

}