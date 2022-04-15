package io.legado.app.ui.book.read.page.entities

data class TextPos(
    var relativePos: Int,
    var lineIndex: Int,
    var charIndex: Int
) {

    fun upData(pos: TextPos) {
        relativePos = pos.relativePos
        lineIndex = pos.lineIndex
        charIndex = pos.charIndex
    }

    fun compare(pos: TextPos): Int {
        return when {
            relativePos < pos.relativePos -> -3
            relativePos > pos.relativePos -> 3
            lineIndex < pos.lineIndex -> -2
            lineIndex > pos.lineIndex -> 2
            charIndex < pos.charIndex -> -1
            charIndex > pos.charIndex -> 1
            else -> 0
        }
    }
}