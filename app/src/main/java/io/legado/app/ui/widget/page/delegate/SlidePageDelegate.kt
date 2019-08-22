package io.legado.app.ui.widget.page.delegate

import android.graphics.Canvas
import android.graphics.Matrix
import io.legado.app.ui.widget.page.PageView

class SlidePageDelegate(pageView: PageView) : PageDelegate(pageView) {

    private val bitmapMatrix = Matrix()

    override fun onScrollStart() {
        val distanceX: Float
        when (direction) {
            Direction.NEXT -> if (isCancel) {
                var dis = viewWidth - startX + touchX
                if (dis > viewWidth) {
                    dis = viewWidth.toFloat()
                }
                distanceX = viewWidth - dis
            } else {
                distanceX = -(touchX + (viewWidth - startX))
            }
            else -> distanceX = if (isCancel) {
                -(touchX - startX)
            } else {
                viewWidth - (touchX - startX)
            }
        }

        startScroll(touchX.toInt(), 0, distanceX.toInt(), 0)
    }

    override fun onDraw(canvas: Canvas) {
        val offsetX = touchX - startX

        if ((direction == Direction.NEXT && offsetX > 0)
            || (direction == Direction.PREV && offsetX < 0)
        ) return

        val distanceX = if (offsetX > 0) offsetX - viewWidth else offsetX + viewWidth
        bitmap?.let {
            bitmapMatrix.setTranslate(distanceX, 0.toFloat())
            canvas.drawBitmap(it, bitmapMatrix, null)
        }
    }

    override fun onScroll() {
        val offsetX = touchX - startX

        if ((direction == Direction.NEXT && offsetX > 0)
            || (direction == Direction.PREV && offsetX < 0)
        ) return

        curPage?.translationX = offsetX
    }

    override fun onScrollStop() {
        curPage?.x = 0.toFloat()

        if (!isCancel) {
            pageView.fillPage(direction)
        }
    }
}