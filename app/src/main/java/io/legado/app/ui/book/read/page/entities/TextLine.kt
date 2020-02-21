package io.legado.app.ui.book.read.page.entities

data class TextLine(
    var text: String = "",
    val textChars: ArrayList<TextChar> = arrayListOf(),
    var lineTop: Int = 0,
    var lineBottom: Int = 0,
    val isTitle: Boolean = false,
    var isReadAloud: Boolean = false
)