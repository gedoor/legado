package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "replace_rules",
    indices = [(Index(value = ["id"]))])
data class ReplaceRule(
                @PrimaryKey(autoGenerate = true)
                var id: Int = 0,
                var name: String? = null,
                var pattern: String? = null,
                var replacement: String? = null,
                var scope: String? = null,
                var isEnabled: Boolean = true,
                var isRegex: Boolean = true,
                @ColumnInfo(name = "sortOrder")
                var order: Int = 0
) : Parcelable


