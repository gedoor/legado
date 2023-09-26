package io.legado.app.ui.book.read.page.entities.column

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.text.StaticLayout
import androidx.annotation.Keep
import io.legado.app.ui.book.read.page.provider.ChapterProvider

/**
 * 评论按钮列
 */
@Keep
data class ReviewColumn(
    override var start: Float,
    override var end: Float,
    val count: Int = 0
) : BaseColumn {

    val countText by lazy {
        if (count > 999) {
            return@lazy "999"
        }
        return@lazy count.toString()
    }

    val path by lazy { Path() }

    fun drawToCanvas(canvas: Canvas, baseLine: Float, height: Float) {
        if (count == 0) return
        path.reset()
        path.moveTo(start + 1, baseLine - height * 2 / 5)
        path.lineTo(start + height / 6, baseLine - height * 0.55f)
        path.lineTo(start + height / 6, baseLine - height * 0.8f)
        path.lineTo(end - 1, baseLine - height * 0.8f)
        path.lineTo(end - 1, baseLine)
        path.lineTo(start + height / 6, baseLine)
        path.lineTo(start + height / 6, baseLine - height / 4)
        path.close()
        ChapterProvider.reviewPaint.style = Paint.Style.STROKE
        canvas.drawPath(path, ChapterProvider.reviewPaint)
        ChapterProvider.reviewPaint.style = Paint.Style.FILL
        val textWidth = StaticLayout.getDesiredWidth(countText, ChapterProvider.reviewPaint)
        canvas.drawText(
            countText,
            start + height / 6 + textWidth * 0.45f,
            baseLine - height / 5,
            ChapterProvider.reviewPaint
        )
    }


}