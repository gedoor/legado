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
    var reviewCount: String,
    var segmentIndex: String,
) : BaseColumn {

    private val countText by lazy {
        if (reviewCount.toInt() >= 99) {
            return@lazy "99+"
        }
        return@lazy reviewCount
    }

    val path by lazy { Path() }

    fun drawToCanvas(canvas: Canvas, baseLine: Float, height: Float) {
        if (reviewCount.toInt() == 0) return
        path.reset()
        path.moveTo(start - height / 6, baseLine - height / 2)
        path.lineTo(start + height / 8, baseLine - height)
        path.lineTo(end, baseLine - height)
        path.lineTo(end, baseLine)
        path.lineTo(start + height / 8, baseLine)
        path.close()
        ChapterProvider.reviewPaint.style = Paint.Style.STROKE
        canvas.drawPath(path, ChapterProvider.reviewPaint)
        ChapterProvider.reviewPaint.style = Paint.Style.FILL
        canvas.drawText(
            countText,
            (start + end) / 2,
            baseLine - height / 3,
            ChapterProvider.reviewPaint
        )
    }


}