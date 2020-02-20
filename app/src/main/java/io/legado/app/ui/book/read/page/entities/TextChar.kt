package io.legado.app.ui.book.read.page.entities

import android.graphics.Point

data class TextChar(
    val charData: String,
    var selected: Boolean = false,
    val leftBottomPosition: Point,
    val rightTopPosition: Point
)