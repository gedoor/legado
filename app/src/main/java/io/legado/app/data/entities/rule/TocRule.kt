package io.legado.app.data.entities.rule

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TocRule(
    var chapterList: String? = null,
    var chapterName: String? = null,
    var chapterUrl: String? = null,
    var isVip: String? = null,
    var updateTime: String? = null,
    var nextTocUrl: String? = null
) : Parcelable