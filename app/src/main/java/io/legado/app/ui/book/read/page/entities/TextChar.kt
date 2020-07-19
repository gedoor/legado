package io.legado.app.ui.book.read.page.entities

data class TextChar(
    val charData: String,
    var start: Float,
    var end: Float,
    var selected: Boolean = false,
    var isImage: Boolean = false
)