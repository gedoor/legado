package io.legado.app.ui.book.read.page.entities

/**
 * 位置信息
 */
data class TextPos(
    var relativePagePos: Int,
    var lineIndex: Int,
    var charIndex: Int
) {

    fun upData(relativePos: Int, lineIndex: Int, charIndex: Int) {
        this.relativePagePos = relativePos
        this.lineIndex = lineIndex
        this.charIndex = charIndex
    }

    fun upData(pos: TextPos) {
        relativePagePos = pos.relativePagePos
        lineIndex = pos.lineIndex
        charIndex = pos.charIndex
    }

    fun compare(pos: TextPos): Int {
        return when {
            relativePagePos < pos.relativePagePos -> -3
            relativePagePos > pos.relativePagePos -> 3
            lineIndex < pos.lineIndex -> -2
            lineIndex > pos.lineIndex -> 2
            charIndex < pos.charIndex -> -1
            charIndex > pos.charIndex -> 1
            else -> 0
        }
    }

    fun compare(relativePos: Int, lineIndex: Int, charIndex: Int): Int {
        return when {
            this.relativePagePos < relativePos -> -3
            this.relativePagePos > relativePos -> 3
            this.lineIndex < lineIndex -> -2
            this.lineIndex > lineIndex -> 2
            this.charIndex < charIndex -> -1
            this.charIndex > charIndex -> 1
            else -> 0
        }
    }
}