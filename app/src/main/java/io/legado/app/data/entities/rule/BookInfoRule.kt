package io.legado.app.data.entities.rule

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class BookInfoRule(
    var init: String? = null,
    var name: String? = null,
    var author: String? = null,
    var intro: String? = null,
    var kind: String? = null,
    var lastChapter: String? = null,
    var updateTime: String? = null,
    var coverUrl: String? = null,
    var tocUrl: String? = null,
    var wordCount: String? = null
) : Parcelable