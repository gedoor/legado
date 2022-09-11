package io.legado.app.ui.book.read.page.entities

import android.text.TextPaint
import io.legado.app.ui.book.read.page.entities.column.BaseColumn
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.utils.textHeight

/**
 * 行信息
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
data class TextLine(
    var text: String = "",
    private val textColumns: ArrayList<BaseColumn> = arrayListOf(),
    var lineTop: Float = 0f,
    var lineBase: Float = 0f,
    var lineBottom: Float = 0f,
    val isTitle: Boolean = false,
    var isParagraphEnd: Boolean = false,
    var isReadAloud: Boolean = false,
    var isImage: Boolean = false
) {

    val columns: List<BaseColumn> get() = textColumns
    val charSize: Int get() = textColumns.size
    val lineStart: Float get() = textColumns.firstOrNull()?.start ?: 0f
    val lineEnd: Float get() = textColumns.lastOrNull()?.end ?: 0f

    fun addColumn(column: BaseColumn) {
        textColumns.add(column)
    }

    fun getColumn(index: Int): BaseColumn {
        return textColumns.getOrElse(index) {
            textColumns.last()
        }
    }

    fun getColumnReverseAt(index: Int): BaseColumn {
        return textColumns[textColumns.lastIndex - index]
    }

    fun getColumnsCount(): Int {
        return textColumns.size
    }

    fun upTopBottom(durY: Float, textPaint: TextPaint) {
        lineTop = ChapterProvider.paddingTop + durY
        lineBottom = lineTop + textPaint.textHeight
        lineBase = lineBottom - textPaint.fontMetrics.descent
    }

    fun isTouch(x: Float, y: Float, relativeOffset: Float): Boolean {
        return y > lineTop + relativeOffset
                && y < lineBottom + relativeOffset
                && x >= lineStart
                && x <= lineEnd
    }
}
