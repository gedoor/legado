package io.legado.app.data.entities

import android.os.Parcelable
import android.text.TextUtils
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
    var name: String? = null,
    var author: String? = null,
    var kind: String? = null,
    var coverUrl: String? = null,
    var intro: String? = null,
    var wordCount: String? = null,
    var latestChapterTitle: String? = null,
    var tocUrl: String = "",                    // 目录页Url (toc=table of Contents)
    var time: Long = 0L,
    var variable: String? = null,
    var bookInfoHtml: String? = null,
    var originOrder: Int = 0
) : Parcelable, BaseBook {

    @IgnoredOnParcel
    @Ignore
    override var variableMap: HashMap<String, String>? = null
        get() = run {
            initVariableMap()
            return field
        }

    private fun initVariableMap() {
        variableMap?.let {
            variableMap = if (TextUtils.isEmpty(variable)) {
                HashMap()
            } else {
                GSON.fromJsonObject(variable)
            }
        }
    }

    override fun putVariable(key: String, value: String) {
        initVariableMap()
        variableMap?.put(key, value)
        variable = GSON.toJson(variableMap)
    }

    fun toBook(): Book {
        val book = Book()
        book.name = name
        book.author = author
        book.kind = kind
        book.bookUrl = bookUrl
        book.origin = origin
        book.originName = originName
        book.wordCount = wordCount
        book.latestChapterTitle = latestChapterTitle
        book.coverUrl = coverUrl
        book.intro = intro
        book.tocUrl = tocUrl
        return book
    }
}