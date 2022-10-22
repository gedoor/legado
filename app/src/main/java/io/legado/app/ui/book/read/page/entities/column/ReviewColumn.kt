package io.legado.app.ui.book.read.page.entities.column

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
        if (count > 99) {
            return@lazy "99+"
        }
        return@lazy count.toString()
    }

    val path by lazy { Path() }

    fun drawToCanvas(canvas: Canvas, baseLine: Float, height: Float) {
        if (count == 0) return
        path.reset()
        path.moveTo(start, baseLine - height / 2)
        path.lineTo(start + height / 6, baseLine - height)
        path.lineTo(end, baseLine - height)
        path.lineTo(end, baseLine)
        path.lineTo(start + height / 6, baseLine)
        path.close()
        ChapterProvider.reviewPaint.style = Paint.Style.STROKE
        canvas.drawPath(path, ChapterProvider.reviewPaint)
        ChapterProvider.reviewPaint.style = Paint.Style.FILL
        canvas.drawText(
            countText,
            (start + end) / 2,
            baseLine - height / 6,
            ChapterProvider.reviewPaint
        )
    }


}