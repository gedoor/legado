package io.legado.app.ui.book.read.page.entities.column

/**
 * 文字列
 */
data class TextColumn(
    override var start: Float,
    override var end: Float,
    val charData: String,
    var selected: Boolean = false,
    var isSearchResult: Boolean = false
) : BaseColumn