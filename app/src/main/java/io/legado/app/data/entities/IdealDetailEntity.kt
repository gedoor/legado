package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import kotlinx.parcelize.Parcelize

@Entity(primaryKeys = ["id", "idealId"], tableName = "book_ideal_list")
@Parcelize
data class IdealDetailEntity(
    var id: Long = 0b1,
    var idealId: Long, // 整个想法的id
    var idealContent: String? = "", // 想法内容
    var userAvatar: String? = "",   // 用户头像
    var assistCount: Int = 0,  // 赞数量
    var replyCount: Int = 0     // 回复数量
) : Parcelable {

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is IdealDetailEntity) return false
        return id == other.id &&
                idealId == other.idealId &&
                idealContent == other.idealContent &&
                userAvatar == other.userAvatar &&
                assistCount == other.assistCount &&
                replyCount == other.replyCount
    }
}