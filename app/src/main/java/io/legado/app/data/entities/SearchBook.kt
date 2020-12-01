package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.*
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "searchBooks",
    indices = [(Index(value = ["bookUrl"], unique = true)),
        (Index(value = ["origin"], unique = false))],
    foreignKeys = [(ForeignKey(
        entity = BookSource::class,
        parentColumns = ["bookSourceUrl"],
        childColumns = ["origin"],
        onDelete = ForeignKey.CASCADE
    ))]
)
data class SearchBook(
    @PrimaryKey
    override var bookUrl: String = "",
    var origin: String = "",                     // 书源规则
    var originName: String = "",
    var type: Int = 0,                          // @BookType
    override var name: String = "",
    override var author: String = "",
    override var kind: String? = null,
    var coverUrl: String? = null,
    var intro: String? = null,
    override var wordCount: String? = null,
    var latestChapterTitle: String? = null,
    var tocUrl: String = "",                    // 目录页Url (toc=table of Contents)
    var time: Long = System.currentTimeMillis(),
    var variable: String? = null,
    var originOrder: Int = 0
): Parcelable, BaseBook, Comparable<SearchBook> {
    
    @Ignore
    @IgnoredOnParcel
    override var infoHtml: String? = null
    
    @Ignore
    @IgnoredOnParcel
    override var tocHtml: String? = null
    
    override fun equals(other: Any?) = other is SearchBook && other.bookUrl == bookUrl
    
    override fun hashCode() = bookUrl.hashCode()
    
    override fun compareTo(other: SearchBook): Int {
        return other.originOrder - this.originOrder
    }
    
    @delegate:Transient
    @delegate:Ignore
    @IgnoredOnParcel
    override val variableMap by lazy {
        GSON.fromJsonObject<HashMap<String, String>>(variable) ?: HashMap()
    }
    
    override fun putVariable(key: String, value: String) {
        variableMap[key] = value
        variable = GSON.toJson(variableMap)
    }
    
    @delegate:Transient
    @delegate:Ignore
    @IgnoredOnParcel
    val origins: LinkedHashSet<String> by lazy { linkedSetOf(origin) }
    
    fun addOrigin(origin: String) {
        origins.add(origin)
    }
    
    fun getDisplayLastChapterTitle(): String {
        latestChapterTitle?.let {
            if (it.isNotEmpty()) {
                return it
            }
        }
        return "无最新章节"
    }
    
    fun toBook() = Book(
        name = name,
        author = author,
        kind = kind,
        bookUrl = bookUrl,
        origin = origin,
        originName = originName,
        type = type,
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