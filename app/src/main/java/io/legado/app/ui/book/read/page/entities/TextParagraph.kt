package io.legado.app.ui.book.read.page.entities

@Suppress("unused")
data class TextParagraph(
    var num: Int,
    val textLines: ArrayList<TextLine> = arrayListOf(),
) : Iterable<TextLine> {
    val text: String get() = textLines.joinToString("") { it.text }
    val length: Int get() = text.length
    val chapterIndices: IntRange get() = first().chapterPosition..last().chapterPosition + last().charSize
    val chapterPosition: Int get() = first().chapterPosition
    val realNum: Int get() = first().paragraphNum
    val isParagraphEnd: Boolean get() = last().isParagraphEnd

    override fun iterator(): Iterator<TextLine> {
        return textLines.iterator()
    }
}
