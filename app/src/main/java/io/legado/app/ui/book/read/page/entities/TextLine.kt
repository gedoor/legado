package io.legado.app.ui.book.read.page.entities

import android.graphics.Canvas
import android.graphics.Paint.FontMetrics
import androidx.annotation.Keep
import io.legado.app.help.book.isImage
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.read.page.ContentTextView
import io.legado.app.ui.book.read.page.entities.TextPage.Companion.emptyTextPage
import io.legado.app.ui.book.read.page.entities.column.BaseColumn
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.utils.PictureMirror
import io.legado.app.utils.dpToPx

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
    var isImage: Boolean = false,
) {

    val columns: List<BaseColumn> get() = textColumns
    val charSize: Int get() = textColumns.size
    val lineStart: Float get() = textColumns.firstOrNull()?.start ?: 0f
    val lineEnd: Float get() = textColumns.lastOrNull()?.end ?: 0f
    val chapterIndices: IntRange get() = chapterPosition..chapterPosition + charSize
    val height: Float inline get() = lineBottom - lineTop
    val pictureMirror: PictureMirror = PictureMirror()
    var isReadAloud: Boolean = false
        set(value) {
            if (field != value) {
                invalidate()
            }
            field = value
        }
    var textPage: TextPage = emptyTextPage
    var isLeftLine = true

    fun addColumn(column: BaseColumn) {
        column.textLine = this
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

    fun upTopBottom(durY: Float, textHeight: Float, fontMetrics: FontMetrics) {
        lineTop = ChapterProvider.paddingTop + durY
        lineBottom = lineTop + textHeight
        lineBase = lineBottom - fontMetrics.descent
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

    fun draw(view: ContentTextView, canvas: Canvas) {
        pictureMirror.draw(canvas, view.width, height.toInt()) {
            drawTextLine(view, this)
        }
    }

    private fun drawTextLine(view: ContentTextView, canvas: Canvas) {
        for (i in columns.indices) {
            columns[i].draw(view, canvas)
        }
        if (ReadBookConfig.underline && !isImage && ReadBook.book?.isImage != true) {
            drawUnderline(canvas)
        }
    }

    /**
     * 绘制下划线
     */
    private fun drawUnderline(canvas: Canvas) {
        val lineY = height - 1.dpToPx()
        canvas.drawLine(
            lineStart + indentWidth,
            lineY,
            lineEnd,
            lineY,
            ChapterProvider.contentPaint
        )
    }

    fun invalidate() {
        invalidateSelf()
        textPage.invalidate()
    }

    fun invalidateSelf() {
        pictureMirror.invalidate()
    }

    fun recyclePicture() {
        pictureMirror.recycle()
    }

    companion object {
        val emptyTextLine = TextLine()
    }

}
