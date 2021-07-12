package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import io.legado.app.utils.GSON
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.fromJsonObject
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
        tableName = "chapters",
        primaryKeys = ["url", "bookUrl"],
        indices = [(Index(value = ["bookUrl"], unique = false)),
            (Index(value = ["bookUrl", "index"], unique = true))],
        foreignKeys = [(ForeignKey(
                entity = Book::class,
                parentColumns = ["bookUrl"],
                childColumns = ["bookUrl"],
                onDelete = ForeignKey.CASCADE
        ))]
)    // 删除书籍时自动删除章节
data class BookChapter(
        var url: String = "",               // 章节地址
        var title: String = "",             // 章节标题
        var baseUrl: String = "",           //用来拼接相对url
        var bookUrl: String = "",           // 书籍地址
        var index: Int = 0,                 // 章节序号
        var resourceUrl: String? = null,    // 音频真实URL
        var tag: String? = null,            //
        var start: Long? = null,            // 章节起始位置
        var end: Long? = null,              // 章节终止位置
        var startFragmentId: String? = null,  //EPUB书籍当前章节的fragmentId
        var endFragmentId: String? = null,    //EPUB书籍下一章节的fragmentId
        var variable: String? = null        //变量
) : Parcelable {

    @delegate:Transient
    @delegate:Ignore
    @IgnoredOnParcel
    val variableMap by lazy {
        GSON.fromJsonObject<HashMap<String, String>>(variable) ?: HashMap()
    }

    fun putVariable(key: String, value: String) {
        variableMap[key] = value
        variable = GSON.toJson(variableMap)
    }

    override fun hashCode() = url.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other is BookChapter) {
            return other.url == url
        }
        return false
    }

    fun getAbsoluteURL():String{
        val pos = url.indexOf(',')
        return if(pos == -1) NetworkUtils.getAbsoluteURL(baseUrl,url)
        else NetworkUtils.getAbsoluteURL(
                baseUrl,
                url.substring(0, pos)
            ) + url.substring(pos)
    }

    fun getFileName(): String = String.format("%05d-%s.nb", index, MD5Utils.md5Encode16(title))

    fun getFontName(): String = String.format("%05d-%s.ttf", index, MD5Utils.md5Encode16(title))
}

