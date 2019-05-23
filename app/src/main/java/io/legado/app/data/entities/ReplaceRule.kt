package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "replace_rules",
    indices = [(Index(value = ["id"]))])
data class ReplaceRule(
                @PrimaryKey(autoGenerate = true)
                val id: Int = 0,
                val summary: String? = null,
                val pattern: String? = null,
                val replacement: String? = null,
                val scope: String? = null,
                val isEnabled: Boolean = true,
                val isRegex: Boolean = true,
                val order: Int = 0
) : Parcelable


