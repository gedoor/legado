package io.legado.app.ui.book.read.page.entities

import android.text.TextPaint
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.ui.book.read.page.provider.ChapterProvider.textHeight

@Suppress("unused")
data class TextLine(
    var text: String = "",
    val textChars: ArrayList<TextChar> = arrayListOf(),
    var lineTop: Float = 0f,
    var lineBase: Float = 0f,
    var lineBottom: Float = 0f,
    val isTitle: Boolean = false,
    var isReadAloud: Boolean = false,
    var isImage: Boolean = false
) {

    val charSize: Int get() = textChars.size

    fun upTopBottom(durY: Float, textPaint: TextPaint) {
        lineTop = ChapterProvider.paddingTop + durY
        lineBottom = lineTop + textPaint.textHeight
        lineBase = lineBottom - textPaint.fontMetrics.descent
    }

    fun getTextChar(index: Int): TextChar {
        return textChars.getOrElse(index) {
            textChars.last()
        }
    }

    fun getTextCharReverseAt(index: Int): TextChar {
        return textChars[textChars.lastIndex - index]
    }

    fun getTextCharsCount(): Int {
        return textChars.size
    }
}
