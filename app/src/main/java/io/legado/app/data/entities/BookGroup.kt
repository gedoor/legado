package io.legado.app.data.entities

import android.content.Context
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.legado.app.R
import io.legado.app.constant.AppConst
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "book_groups")
data class BookGroup(
    @PrimaryKey
    val groupId: Long = 0b1,
    var groupName: String,
    var order: Int = 0,
    var show: Boolean = true
) : Parcelable {

    fun getDefaultName(context: Context): String {
        return when (groupId) {
            AppConst.bookGroupAllId -> context.getString(R.string.all)
            AppConst.bookGroupAudioId -> context.getString(R.string.audio)
            AppConst.bookGroupLocalId -> context.getString(R.string.local)
            AppConst.bookGroupNoneId -> context.getString(R.string.no_group)
            else -> groupName
        }
    }

}