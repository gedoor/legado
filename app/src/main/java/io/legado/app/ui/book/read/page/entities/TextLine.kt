package io.legado.app.ui.book.read.page.entities

import android.text.TextPaint
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.utils.textHeight

/**
 * 行信息
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
data class TextLine(
    var text: String = "",
    val textChars: ArrayList<TextColumn> = arrayListOf(),
    var lineTop: Float = 0f,
    var lineBase: Float = 0f,
    var lineBottom: Float = 0f,
    val isTitle: Boolean = false,
    var isParagraphEnd: Boolean = false,
    var isReadAloud: Boolean = false,
    var isImage: Boolean = false
) {

    val charSize: Int get() = textChars.size
    val lineStart: Float get() = textChars.firstOrNull()?.start ?: 0f
    val lineEnd: Float get() = textChars.lastOrNull()?.end ?: 0f

    fun upTopBottom(durY: Float, textPaint: TextPaint) {
        lineTop = ChapterProvider.paddingTop + durY
        lineBottom = lineTop + textPaint.textHeight
        lineBase = lineBottom - textPaint.fontMetrics.descent
    }

    fun getTextChar(index: Int): TextColumn {
        return textChars.getOrElse(index) {
            textChars.last()
        }
    }

    fun getTextCharReverseAt(index: Int): TextColumn {
        return textChars[textChars.lastIndex - index]
    }

    fun getTextCharsCount(): Int {
        return textChars.size
    }

    fun isTouch(x: Float, y: Float, relativeOffset: Float): Boolean {
        return y > lineTop + relativeOffset
                && y < lineBottom + relativeOffset
                && x >= lineStart
                && x <= lineEnd
    }
}
