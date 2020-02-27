package io.legado.app.ui.book.read.page.entities

data class TextChar(
    val charData: String,
    val start: Float,
    val end: Float,
    var selected: Boolean = false
)