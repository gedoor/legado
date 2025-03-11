package io.legado.app.ui.book.manga.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MangaColorFilterConfig(
    var r: Int = 0,
    var g: Int = 0,
    var b: Int = 0,
    var a: Int = 0,
    var l: Int = 0
) :
    Parcelable