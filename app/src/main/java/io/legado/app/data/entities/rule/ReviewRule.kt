package io.legado.app.data.entities.rule

import android.os.Parcelable
import com.google.gson.JsonDeserializer
import io.legado.app.utils.INITIAL_GSON
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReviewRule(
    var reviewCountList: String? = null,    // 段评数量列表
    var reviewSegmentId: String? = null,    // 段评数量索引
    var reviewCount: String? = null,        // 段评数量

    var reviewUrl: String? = null,          // 段评列表URL
    var reviewList: String? = null,         // 段评列表规则
    var reviewContent: String? = null,      // 段评内容规则
    var reviewImgUrl: String? = null,       // 段评配图规则
    var reviewPostAvatar: String? = null,   // 段评发布者头像
    var reviewPostName: String? = null,     // 段评发布者名称
    var reviewPostTime: String? = null,     // 段评发布时间
    var reviewLikeCount: String? = null,    // 段评点赞数量
    var quoteReviewDefault: String? = null, // 段评默认展开规则
    var quoteReviewCount: String? = null,   // 段评展开数量规则
    var quoteReviewUrl: String? = null,     // 段评展开URL
) : Parcelable {

    companion object {

        val jsonDeserializer = JsonDeserializer<ReviewRule?> { json, _, _ ->
            when {
                json.isJsonObject -> INITIAL_GSON.fromJson(json, ReviewRule::class.java)
                json.isJsonPrimitive -> INITIAL_GSON.fromJson(json.asString, ReviewRule::class.java)
                else -> null
            }
        }

    }

}
