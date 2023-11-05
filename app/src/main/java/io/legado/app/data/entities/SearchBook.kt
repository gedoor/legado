package io.legado.app.data.entities

import android.content.Context
import android.os.Parcelable
import androidx.room.*
import io.legado.app.R
import io.legado.app.constant.BookType
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
    /** 书源 */
    var origin: String = "",
    var originName: String = "",
    /** BookType */
    var type: Int = BookType.text,
    override var name: String = "",
    override var author: String = "",
    override var kind: String? = null,
    var coverUrl: String? = null,
    var intro: String? = null,
    override var wordCount: String? = null,
    var latestChapterTitle: String? = null,
    /** 目录页Url (toc=table of Contents) */
    var tocUrl: String = "",
    var time: Long = System.currentTimeMillis(),
    override var variable: String? = null,
    var originOrder: Int = 0,
    var chapterWordCountText: String? = null,
    @ColumnInfo(defaultValue = "-1")
    var chapterWordCount: Int = -1,
    @ColumnInfo(defaultValue = "-1")
    var respondTime: Int = -1
) : Parcelable, BaseBook, Comparable<SearchBook> {

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
    override val variableMap: HashMap<String, String> by lazy {
        GSON.fromJsonObject<HashMap<String, String>>(variable).getOrNull() ?: HashMap()
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

    fun trimIntro(context: Context): String {
        val trimIntro = intro?.trim()
        return if (trimIntro.isNullOrEmpty()) {
            context.getString(R.string.intro_show_null)
        } else {
            context.getString(R.string.intro_show, trimIntro)
        }
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
        this.tocHtml = this@SearchBook.tocHtml
    }
}