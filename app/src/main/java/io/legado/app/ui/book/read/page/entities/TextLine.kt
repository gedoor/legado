package io.legado.app.ui.book.read.page.entities

import android.text.TextPaint
import androidx.annotation.Keep
import io.legado.app.ui.book.read.page.entities.column.BaseColumn
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.utils.textHeight

/**
 * 行信息
 */
@Keep
@Suppress("unused", "MemberVisibilityCanBePrivate")
data class TextLine(
    var text: String = "",
    private val textColumns: ArrayList<BaseColumn> = arrayListOf(),
    var lineTop: Float = 0f,
    var lineBase: Float = 0f,
    var lineBottom: Float = 0f,
    var indentWidth: Float = 0f,
    var paragraphNum: Int = 0,
    var chapterPosition: Int = 0,
    var pagePosition: Int = 0,
    val isTitle: Boolean = false,
    var isParagraphEnd: Boolean = false,
    var isReadAloud: Boolean = false,
    var isImage: Boolean = false
) {

    val columns: List<BaseColumn> get() = textColumns
    val charSize: Int get() = textColumns.size
    val lineStart: Float get() = textColumns.firstOrNull()?.start ?: 0f
    val lineEnd: Float get() = textColumns.lastOrNull()?.end ?: 0f
    val chapterIndices: IntRange get() = chapterPosition..chapterPosition + charSize

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

    fun isTouchY(y: Float, relativeOffset: Float): Boolean {
        return y > lineTop + relativeOffset
                && y < lineBottom + relativeOffset
    }

    fun isVisible(relativeOffset: Float): Boolean {
        val top = lineTop + relativeOffset
        val bottom = lineBottom + relativeOffset
        val width = bottom - top
        val visibleTop = ChapterProvider.paddingTop
        val visibleBottom = ChapterProvider.visibleBottom
        val visible = when {
            // 完全可视
            top >= visibleTop && bottom <= visibleBottom -> true
            top <= visibleTop && bottom >= visibleBottom -> true
            // 上方第一行部分可视
            top < visibleTop && bottom > visibleTop && bottom < visibleBottom -> {
                if (isImage) {
                    true
                } else {
                    val visibleRate = (bottom - visibleTop) / width
                    visibleRate > 0.6
                }
            }
            // 下方第一行部分可视
            top > visibleTop && top < visibleBottom && bottom > visibleBottom -> {
                if (isImage) {
                    true
                } else {
                    val visibleRate = (visibleBottom - top) / width
                    visibleRate > 0.6
                }
            }
            // 不可视
            else -> false
        }
        return visible
    }

}
