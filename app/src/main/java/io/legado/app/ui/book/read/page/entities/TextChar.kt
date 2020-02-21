package io.legado.app.ui.book.read.page.entities

data class TextChar(
    val charData: String,
    var selected: Boolean = false,
    val leftBottomPosition: TextPoint,
    val rightTopPosition: TextPoint
)