package io.legado.app.ui.book.read.page.entities

import android.graphics.Point

data class TextChar(
    val charData: Char,
    var selected: Boolean = false,
    var isReadAloud: Boolean = false,
    val leftBottomPosition: Point,
    val rightTopPosition: Point
)