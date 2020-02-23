package io.legado.app.ui.book.read.page.entities

data class TextLine(
    var text: String = "",
    val textChars: ArrayList<TextChar> = arrayListOf(),
    var lineTop: Float = 0f,
    var lineBottom: Float = 0f,
    val isTitle: Boolean = false,
    var isReadAloud: Boolean = false
)