package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "rssSources", indices = [(Index(value = ["sourceUrl"], unique = false))])
data class RssSource(
    @PrimaryKey
    var sourceUrl: String = "",
    var sourceName: String = "",
    var sourceIcon: String = "",
    var sourceGroup: String? = null,
    var enabled: Boolean = true,
    //列表规则
    var ruleArticles: String? = null,
    var ruleTitle: String? = null,
    var rulePubDate: String? = null,
    //类别规则
    var ruleCategories: String? = null,
    //webView规则
    var ruleDescription: String? = null,
    var ruleImage: String? = null,
    var ruleLink: String? = null,
    var ruleContent: String? = null,
    var enableJs: Boolean = false,
    var loadWithBaseUrl: Boolean = true,
    var customOrder: Int = 0
) : Parcelable