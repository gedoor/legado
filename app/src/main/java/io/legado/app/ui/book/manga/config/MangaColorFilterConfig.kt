package io.legado.app.ui.book.manga.config

import io.legado.app.utils.GSON

data class MangaColorFilterConfig(
    var r: Int = 0,
    var g: Int = 0,
    var b: Int = 0,
    var a: Int = 0,
    var l: Int = 0
) {
    fun toJson(): String {
        if (r == 0 && g == 0 && b == 0 && a == 0 && l == 0) {
            return ""
        }
        return GSON.toJson(this)
    }
}
