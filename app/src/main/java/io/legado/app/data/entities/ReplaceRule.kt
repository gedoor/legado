package io.legado.app.data.entities

import android.os.Parcelable
import android.text.TextUtils
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@Parcelize
@Entity(
    tableName = "replace_rules",
    indices = [(Index(value = ["id"]))]
)
data class ReplaceRule(
    @PrimaryKey(autoGenerate = true)
    var id: Long = System.currentTimeMillis(),
    var name: String = "",
    var group: String? = null,
    var pattern: String = "",
    var replacement: String = "",
    var scope: String? = null,
    var isEnabled: Boolean = true,
    var isRegex: Boolean = true,
    @ColumnInfo(name = "sortOrder")
    var order: Int = 0
) : Parcelable {


    override fun equals(other: Any?): Boolean {
        if (other is ReplaceRule) {
            return other.id == id
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    fun isValid(): Boolean{
        if (TextUtils.isEmpty(pattern)){
            return false
        }
        //判断正则表达式是否正确
        if (isRegex){
            try {
                Pattern.compile(pattern)
            } catch (ex: PatternSyntaxException) {
                return false
            }
        }
        return true
    }
}