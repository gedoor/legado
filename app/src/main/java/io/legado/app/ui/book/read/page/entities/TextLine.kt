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
    val textColumns: ArrayList<TextColumn> = arrayListOf(),
    val reviewCount: Int = 0,
    var lineTop: Float = 0f,
    var lineBase: Float = 0f,
    var lineBottom: Float = 0f,
    val isTitle: Boolean = false,
    var isParagraphEnd: Boolean = false,
    var isReadAloud: Boolean = false,
    var isImage: Boolean = false
) {

    val charSize: Int get() = textColumns.size
    val lineStart: Float get() = textColumns.firstOrNull()?.start ?: 0f
    val lineEnd: Float get() = textColumns.lastOrNull()?.end ?: 0f

    fun upTopBottom(durY: Float, textPaint: TextPaint) {
        lineTop = ChapterProvider.paddingTop + durY
        lineBottom = lineTop + textPaint.textHeight
        lineBase = lineBottom - textPaint.fontMetrics.descent
    }

    fun getTextColumn(index: Int): TextColumn {
        return textColumns.getOrElse(index) {
            textColumns.last()
        }
    }

    fun getTextColumnReverseAt(index: Int): TextColumn {
        return textColumns[textColumns.lastIndex - index]
    }

    fun getTextColumnsCount(): Int {
        return textColumns.size
    }

    fun isTouch(x: Float, y: Float, relativeOffset: Float): Boolean {
        return y > lineTop + relativeOffset
                && y < lineBottom + relativeOffset
                && x >= lineStart
                && x <= lineEnd
    }
}
