package io.legado.app.ui.widget.page.delegate

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.GradientDrawable
import io.legado.app.ui.widget.page.PageView

class CoverPageDelegate(pageView: PageView) : PageDelegate(pageView) {

    private val shadowDrawableR: GradientDrawable
    private val shadowDrawableL: GradientDrawable
    private val bitmapMatrix = Matrix()

    init {
        val shadowColors = intArrayOf(0x66111111, 0x00000000)
        shadowDrawableR = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, shadowColors
        )
        shadowDrawableR.gradientType = GradientDrawable.LINEAR_GRADIENT

        shadowDrawableL = GradientDrawable(
            GradientDrawable.Orientation.RIGHT_LEFT, shadowColors
        )
        shadowDrawableL.gradientType = GradientDrawable.LINEAR_GRADIENT
    }

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

        scroller.startScroll(touchX.toInt(), 0, distanceX.toInt(), 0, getDuration(distanceX))
        start()
    }

    override fun onScrollStop() {
        if (!isCancel) {
            pageView.fillPage(direction)
        }
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

            addShadow(distanceX.toInt(), canvas)
        }
    }

    private fun addShadow(left: Int, canvas: Canvas) {
        if (left < 0) {
            shadowDrawableR.setBounds(left + viewWidth, 0, left + viewWidth + 30, viewHeight)
            shadowDrawableR.draw(canvas)
        } else {
            shadowDrawableL.setBounds(left - 30, 0, left, viewHeight)
            shadowDrawableL.draw(canvas)
        }
    }
}
