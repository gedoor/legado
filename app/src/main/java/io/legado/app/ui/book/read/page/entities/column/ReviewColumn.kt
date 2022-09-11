package io.legado.app.ui.book.read.page.entities.column

import android.graphics.Canvas
import android.graphics.Paint
import android.text.StaticLayout
import android.text.TextPaint

/**
 * 评论按钮列
 */
data class ReviewColumn(
    override var start: Float,
    override var end: Float,
    var count: Int = 0
) : BaseColumn {

    fun getCountText(): String {
        if (count > 99) {
            return "99+"
        }
        return count.toString()
    }

    fun drawToCanvas(canvas: Canvas, paint: Paint, countPaint: TextPaint, y: Float) {
        if (count == 0) return
        canvas.drawLine(
            start,
            y - paint.textSize * 2 / 5,
            start + paint.textSize / 6,
            y - paint.textSize / 4,
            paint
        )
        canvas.drawLine(
            start,
            y - paint.textSize * 0.38F,
            start + paint.textSize / 6,
            y - paint.textSize * 0.55F,
            paint
        )
        canvas.drawLine(
            start + paint.textSize / 6,
            y - paint.textSize / 4,
            start + paint.textSize / 6,
            y,
            paint
        )
        canvas.drawLine(
            start + paint.textSize / 6,
            y - paint.textSize * 0.55F,
            start + paint.textSize / 6,
            y - paint.textSize * 0.8F,
            paint
        )
        canvas.drawLine(
            start + paint.textSize / 6,
            y,
            start + paint.textSize * 1.6F,
            y,
            paint
        )
        canvas.drawLine(
            start + paint.textSize / 6,
            y - paint.textSize * 0.8F,
            start + paint.textSize * 1.6F,
            y - paint.textSize * 0.8F,
            paint
        )
        canvas.drawLine(
            start + paint.textSize * 1.6F,
            y - paint.textSize * 0.8F,
            start + paint.textSize * 1.6F,
            y,
            paint
        )
        val text = getCountText()
        val textWidth = StaticLayout.getDesiredWidth(getCountText(), countPaint)
        canvas.drawText(
            text,
            start + paint.textSize * 0.87F - textWidth / 2,
            y - paint.textSize / 6,
            countPaint
        )
    }


}