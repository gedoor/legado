package io.legado.app.ui.book.read.page.entities

data class TextLine(
    var text: String = "",
    val textChars: ArrayList<TextChar> = arrayListOf(),
    var lineTop: Float = 0f,
    var lineBase: Float = 0f,
    var lineBottom: Float = 0f,
    val isTitle: Boolean = false,
    var isReadAloud: Boolean = false
) {

    fun addTextChar(charData: String, start: Float, end: Float) {
        textChars.add(TextChar(charData, start = start, end = end))
    }

    fun getTextCharAt(index: Int): TextChar {
        return textChars[index]
    }

    fun getTextCharReverseAt(index: Int): TextChar {
        return textChars[textChars.lastIndex - index]
    }

    fun getTextCharsCount(): Int {
        return textChars.size
    }
}
