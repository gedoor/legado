package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "rssSources")
data class RssSource(
    var sourceName: String = "",
    @PrimaryKey
    var sourceUrl: String = "",
    var iconUrl: String = "",
    var enabled: Boolean = true,
    var ruleGuid: String? = null,
    var ruleTitle: String? = null,
    var ruleAuthor: String? = null,
    var rulePubDate: String? = null,
    //类别
    var ruleCategories: String? = null,
    //描述
    var ruleDescription: String? = null,
    var ruleImage: String? = null,
    var ruleContent: String? = null,
    var ruleLink: String? = null,
    var customOrder: Int = 0
) : Parcelable