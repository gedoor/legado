package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import io.legado.app.help.RuleBigDataHelp
import io.legado.app.model.analyzeRule.RuleDataInterface
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "reviews",
    indices = [(Index(value = ["bookUrl"], unique = false)),
        (Index(value = ["chapterUrl"], unique = false))],
    foreignKeys = [(ForeignKey(
        entity = Book::class,
        parentColumns = ["bookUrl"],
        childColumns = ["bookUrl"],
        onDelete = ForeignKey.CASCADE
    ))]
)
data class BookReview(
    @PrimaryKey(autoGenerate = true)
    var reviewId: Int? = null,              // 自增id
    var reviewCountUrl: String = "",        // 段评数量URL
    var reviewCountList: String? = null,    // 段评数量列表
    var reviewSegmentId: String = "",       // 段评数量索引
    var reviewCount: String = "",           // 段评数量

    var reviewUrl: String? = null,          // 段评URL
    var reviewList: String? = null,         // 段评列表
    var reviewContent: String? = null,      // 段评内容
    var reviewImgUrl: String? = null,       // 段评配图
    var reviewPostAvatar: String? = null,   // 段评发布者头像
    var reviewPostName: String? = null,     // 段评发布者名称
    var reviewPostTime: String? = null,     // 段评发布时间
    var reviewLikeCount: String? = null,    // 段评点赞数量
    var quoteReviewDefault: String? = null, // 段评默认展开规则
    var quoteReviewCount: String? = null,   // 段评展开数量规则
    var quoteReviewUrl: String? = null,     // 段评展开URL

    var baseUrl: String = "",               // 用来拼接相对url
    var bookUrl: String = "",               // 书籍地址
    var chapterUrl: String = "",            // 章节地址
    var variable: String? = null,           // 变量
    var isReviewChild: Boolean? = false,    // 标记子评论
) : Parcelable, RuleDataInterface {


    @delegate:Transient
    @delegate:Ignore
    @IgnoredOnParcel
    override val variableMap: HashMap<String, String> by lazy {
        GSON.fromJsonObject<HashMap<String, String>>(variable).getOrNull() ?: hashMapOf()
    }

    override fun putVariable(key: String, value: String?): Boolean {
        if (super.putVariable(key, value)) {
            variable = GSON.toJson(variableMap)
        }
        return true
    }

    override fun putBigVariable(key: String, value: String?) {
        RuleBigDataHelp.putChapterReviewVariable(bookUrl, reviewCountUrl, key, value)
    }

    override fun getBigVariable(key: String): String? {
        return RuleBigDataHelp.getChapterReviewVariable(bookUrl, reviewCountUrl, key)
    }

    override fun hashCode() = reviewSegmentId.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other is BookReview) {
            return other.reviewSegmentId == reviewSegmentId
                    && other.reviewCountUrl == reviewCountUrl
        }
        return false
    }
}
