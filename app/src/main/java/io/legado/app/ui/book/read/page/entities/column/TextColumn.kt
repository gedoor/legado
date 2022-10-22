package io.legado.app.ui.book.read.page.entities.column

import androidx.annotation.Keep

/**
 * 文字列
 */
@Keep
data class TextColumn(
    override var start: Float,
    override var end: Float,
    val charData: String,
    var selected: Boolean = false,
    var isSearchResult: Boolean = false
) : BaseColumn