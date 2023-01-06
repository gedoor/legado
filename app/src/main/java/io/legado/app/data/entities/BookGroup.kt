package io.legado.app.data.entities

import android.content.Context
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.help.config.AppConfig
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "book_groups")
data class BookGroup(
    @PrimaryKey
    val groupId: Long = 0b1,
    var groupName: String,
    var cover: String? = null,
    var order: Int = 0,
    var show: Boolean = true,
    @ColumnInfo(defaultValue = "-1")
    var bookSort: Int = -1
) : Parcelable {

    fun getManageName(context: Context): String {
        return when (groupId) {
            AppConst.bookGroupAllId -> "$groupName(${context.getString(R.string.all)})"
            AppConst.bookGroupAudioId -> "$groupName(${context.getString(R.string.audio)})"
            AppConst.bookGroupLocalId -> "$groupName(${context.getString(R.string.local)})"
            AppConst.bookGroupNetNoneId -> "$groupName(${context.getString(R.string.net_no_group)})"
            AppConst.bookGroupLocalNoneId -> "$groupName(${context.getString(R.string.local_no_group)})"
            AppConst.bookGroupErrorId -> "$groupName(${context.getString(R.string.update_book_fail)})"
            else -> groupName
        }
    }

    fun getRealBookSort(): Int {
        if (bookSort < 0) {
            return AppConfig.bookshelfSort
        }
        return bookSort
    }

    override fun hashCode(): Int {
        return groupId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is BookGroup) {
            return other.groupId == groupId
                    && other.groupName == groupName
                    && other.cover == cover
                    && other.order == order
                    && other.show == show
                    && other.bookSort == bookSort
        }
        return false
    }

}