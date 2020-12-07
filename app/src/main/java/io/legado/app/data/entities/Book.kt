package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.*
import io.legado.app.App
import io.legado.app.constant.AppPattern
import io.legado.app.constant.BookType
import io.legado.app.help.AppConfig
import io.legado.app.service.help.ReadBook
import io.legado.app.utils.GSON
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.fromJsonObject
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.nio.charset.Charset
import kotlin.math.max
import kotlin.math.min

@Parcelize
@TypeConverters(Book.Converters::class)
@Entity(
    tableName = "books",
    indices = [Index(value = ["name", "author"], unique = true)]
)
data class Book(
    @PrimaryKey
    override var bookUrl: String = "",          // 详情页Url(本地书源存储完整文件路径)
    var tocUrl: String = "",                    // 目录页Url (toc=table of Contents)
    var origin: String = BookType.local,        // 书源URL(默认BookType.local)
    var originName: String = "",                //书源名称 or 本地书籍文件名
    override var name: String = "",                      // 书籍名称(书源获取)
    override var author: String = "",                    // 作者名称(书源获取)
    override var kind: String? = null,          // 分类信息(书源获取)
    var customTag: String? = null,              // 分类信息(用户修改)
    var coverUrl: String? = null,               // 封面Url(书源获取)
    var customCoverUrl: String? = null,         // 封面Url(用户修改)
    var intro: String? = null,                  // 简介内容(书源获取)
    var customIntro: String? = null,            // 简介内容(用户修改)
    var charset: String? = null,                // 自定义字符集名称(仅适用于本地书籍)
    var type: Int = 0,                          // 0:text 1:audio
    var group: Long = 0,                         // 自定义分组索引号
    var latestChapterTitle: String? = null,     // 最新章节标题
    var latestChapterTime: Long = System.currentTimeMillis(),            // 最新章节标题更新时间
    var lastCheckTime: Long = System.currentTimeMillis(),                // 最近一次更新书籍信息的时间
    var lastCheckCount: Int = 0,                // 最近一次发现新章节的数量
    var totalChapterNum: Int = 0,               // 书籍目录总数
    var durChapterTitle: String? = null,        // 当前章节名称
    var durChapterIndex: Int = 0,               // 当前章节索引
    var durChapterPos: Int = 0,                 // 当前阅读的进度(首行字符的索引位置)
    var durChapterTime: Long = System.currentTimeMillis(),               // 最近一次阅读书籍的时间(打开正文的时间)
    override var wordCount: String? = null,
    var canUpdate: Boolean = true,              // 刷新书架时更新书籍信息
    var order: Int = 0,                         // 手动排序
    var originOrder: Int = 0,                   //书源排序
    var variable: String? = null,               // 自定义书籍变量信息(用于书源规则检索书籍信息)
    var readConfig: ReadConfig? = null
) : Parcelable, BaseBook {

    fun isLocalBook(): Boolean {
        return origin == BookType.local
    }

    fun isLocalTxt(): Boolean {
        return isLocalBook() && originName.endsWith(".txt", true)
    }

    fun isEpub(): Boolean {
        return originName.endsWith(".epub", true)
    }

    fun isOnLineTxt(): Boolean {
        return !isLocalBook() && type == 0
    }

    override fun equals(other: Any?): Boolean {
        if (other is Book) {
            return other.bookUrl == bookUrl
        }
        return false
    }

    override fun hashCode(): Int {
        return bookUrl.hashCode()
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

    @Ignore
    @IgnoredOnParcel
    override var infoHtml: String? = null

    @Ignore
    @IgnoredOnParcel
    override var tocHtml: String? = null

    fun getRealAuthor() = author.replace(AppPattern.authorRegex, "")

    fun getUnreadChapterNum() = max(totalChapterNum - durChapterIndex - 1, 0)

    fun getDisplayCover() = if (customCoverUrl.isNullOrEmpty()) coverUrl else customCoverUrl

    fun getDisplayIntro() = if (customIntro.isNullOrEmpty()) intro else customIntro

    fun fileCharset(): Charset {
        return charset(charset ?: "UTF-8")
    }

    private fun config(): ReadConfig {
        if (readConfig == null) {
            readConfig = ReadConfig()
        }
        return readConfig!!
    }

    fun setUseReplaceRule(useReplaceRule: Boolean) {
        config().useReplaceRule = useReplaceRule
    }

    fun getUseReplaceRule(): Boolean {
        return config().useReplaceRule
    }

    fun getReSegment(): Boolean {
        return config().reSegment
    }

    fun setReSegment(reSegment: Boolean) {
        config().reSegment = reSegment
    }

    fun getPageAnim(): Int {
        return config().pageAnim
    }

    fun setPageAnim(pageAnim: Int) {
        config().pageAnim = pageAnim
    }

    fun getFolderName(): String {
        //防止书名过长,只取9位
        var folderName = name.replace(AppPattern.fileNameRegex, "")
        folderName = folderName.substring(0, min(9, folderName.length))
        return folderName + MD5Utils.md5Encode16(bookUrl)
    }

    fun toSearchBook() = SearchBook(
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
        this.infoHtml = this@Book.infoHtml
        this.tocHtml = this@Book.tocHtml
    }

    fun changeTo(newBook: Book) {
        newBook.group = group
        newBook.order = order
        newBook.customCoverUrl = customCoverUrl
        newBook.customIntro = customIntro
        newBook.customTag = customTag
        newBook.canUpdate = canUpdate
        newBook.readConfig = readConfig
        delete()
        App.db.bookDao.insert(newBook)
    }

    fun delete() {
        if (ReadBook.book?.bookUrl == bookUrl) {
            ReadBook.book = null
        }
        App.db.bookDao.delete(this)
    }

    fun upInfoFromOld(oldBook: Book?) {
        oldBook?.let {
            group = oldBook.group
            durChapterIndex = oldBook.durChapterIndex
            durChapterPos = oldBook.durChapterPos
            durChapterTitle = oldBook.durChapterTitle
            customCoverUrl = oldBook.customCoverUrl
            customIntro = oldBook.customIntro
            order = oldBook.order
            if (coverUrl.isNullOrEmpty()) {
                coverUrl = oldBook.getDisplayCover()
            }
        }
    }

    @Parcelize
    data class ReadConfig(
        var pageAnim: Int = -1,
        var reSegment: Boolean = false,
        var useReplaceRule: Boolean = AppConfig.replaceEnableDefault,         // 正文使用净化替换规则
    ) : Parcelable

    class Converters {

        @TypeConverter
        fun readConfigToString(config: ReadConfig?): String = GSON.toJson(config)

        @TypeConverter
        fun stringToReadConfig(json: String?) = GSON.fromJsonObject<ReadConfig>(json)
    }
}