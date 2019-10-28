package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "searchBooks", indices = [(Index(value = ["bookUrl"], unique = true))])
data class SearchBook(
    @PrimaryKey
    var bookUrl: String = "",
    var origin: String = "",                     // 书源规则
    var originName: String = "",
    var name: String = "",
    var author: String = "",
    override var kind: String? = null,
    var coverUrl: String? = null,
    var intro: String? = null,
    override var wordCount: String? = null,
    var latestChapterTitle: String? = null,
    var tocUrl: String = "",                    // 目录页Url (toc=table of Contents)
    var time: Long = System.currentTimeMillis(),
    var variable: String? = null,
    var originOrder: Int = 0
) : Parcelable, BaseBook, Comparable<SearchBook> {

    @Ignore
    @IgnoredOnParcel
    override var infoHtml: String? = null

    @Ignore
    @IgnoredOnParcel
    override var tocHtml: String? = null

    override fun equals(other: Any?): Boolean {
        if (other is SearchBook) {
            if (other.bookUrl == bookUrl) {
                return true
            }
        }
        return false
    }

    override fun hashCode(): Int {
        return bookUrl.hashCode()
    }

    override fun compareTo(other: SearchBook): Int {
        return other.originOrder - this.originOrder
    }

    @IgnoredOnParcel
    @Ignore
    override var variableMap: HashMap<String, String> = GSON.fromJsonObject(variable) ?: HashMap()

    override fun putVariable(key: String, value: String) {
        variableMap[key] = value
        variable = GSON.toJson(variableMap)
    }

    @Ignore
    @IgnoredOnParcel
    var origins: LinkedHashSet<String>? = null
        private set

    fun addOrigin(origin: String) {
        if (origins == null) {
            origins = linkedSetOf()
        }
        origins?.add(origin)
    }

    fun toBook(): Book {
        return Book(
            name = name,
            author = author,
            kind = kind,
            bookUrl = bookUrl,
            origin = origin,
            originName = originName,
            wordCount = wordCount,
            latestChapterTitle = latestChapterTitle,
            coverUrl = coverUrl,
            intro = intro,
            tocUrl = tocUrl,
            originOrder = originOrder,
            variable = variable
        ).apply {
            this.infoHtml = this@SearchBook.infoHtml
            this.tocUrl = this@SearchBook.tocUrl
        }
    }
}