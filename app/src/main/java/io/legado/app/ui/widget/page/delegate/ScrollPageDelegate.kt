package io.legado.app.ui.widget.page.delegate

import android.graphics.Canvas
import android.graphics.Matrix
import io.legado.app.ui.widget.page.PageView

class ScrollPageDelegate(pageView: PageView) : PageDelegate(pageView) {

    private val bitmapMatrix = Matrix()

    override fun onScrollStart() {
        if (!atTop && !atBottom) {
            stopScroll()
            return
        }
        val distanceY: Float
        when (direction) {
            Direction.NEXT -> if (isCancel) {
                var dis = viewHeight - startY + touchY
                if (dis > viewHeight) {
                    dis = viewHeight.toFloat()
                }
                distanceY = viewHeight - dis
            } else {
                distanceY = -(touchY + (viewHeight - startY))
            }
            else -> distanceY = if (isCancel) {
                -(touchY - startY)
            } else {
                viewWidth - (touchY - startY)
            }
        }

        startScroll(0, touchY.toInt(), 0, distanceY.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        if (atTop || atBottom) {
            val offsetY = touchY - startY

            if ((direction == Direction.NEXT && offsetY > 0)
                || (direction == Direction.PREV && offsetY < 0)
            ) return

            val distanceY = if (offsetY > 0) offsetY - viewHeight else offsetY + viewHeight
            if (atTop && direction == Direction.PREV) {
                bitmap?.let {
                    bitmapMatrix.setTranslate(0.toFloat(), distanceY)
                    canvas.drawBitmap(it, bitmapMatrix, null)
                }
            } else if (atBottom && direction == Direction.NEXT) {
                bitmap?.let {
                    bitmapMatrix.setTranslate(0.toFloat(), distanceY)
                    canvas.drawBitmap(it, bitmapMatrix, null)
                }
            }
        }
    }

    override fun onScrollStop() {
        if (!isCancel) {
            pageView.fillPage(direction)
        }
    }

}