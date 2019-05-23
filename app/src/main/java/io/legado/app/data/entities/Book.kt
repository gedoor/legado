package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.*
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "books",
    indices = [(Index(value = ["url"]))])
data class Book(@PrimaryKey
                var url: String = "",
                var name: String = "",
                var tag: String = "",
                var author: String? = null,
                var coverUrl: String? = null,
                var userCoverUrl: String? = null,
                var introduction: String? = null,
                var charset: String? = null,
                var type: Int = 0,   // 0: text, 1: audio
                var group: Int = 0,  // fenqu
                var latestChapterName: String? = null,
                var lastUpdateTime: Long? = null,
                var latestChapterTime: Long? = null,
                var durChapterIndex: Int = 0,
                var durChapterPage: Int = 0,
                var totalChapterNum: Int = 0,
                var hasNewChapter: Boolean = false,
                var canUpdate: Boolean = true
                ) : Parcelable {

    fun getUnreadChapterNum() = Math.max(totalChapterNum - durChapterIndex - 1, 0)

}