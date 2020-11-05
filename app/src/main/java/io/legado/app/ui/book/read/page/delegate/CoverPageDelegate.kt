package io.legado.app.ui.book.read.page.delegate

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.GradientDrawable
import io.legado.app.ui.book.read.page.PageView

class CoverPageDelegate(pageView: PageView) : HorizontalPageDelegate(pageView) {
    private val bitmapMatrix = Matrix()
    private val shadowDrawableR: GradientDrawable

    init {
        val shadowColors = intArrayOf(0x66111111, 0x00000000)
        shadowDrawableR = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, shadowColors
        )
        shadowDrawableR.gradientType = GradientDrawable.LINEAR_GRADIENT
    }

    override fun onDraw(canvas: Canvas) {
        if (!isRunning) return
        val offsetX = touchX - startX

        if ((mDirection == Direction.NEXT && offsetX > 0)
            || (mDirection == Direction.PREV && offsetX < 0)
        ) {
            return
        }

        val distanceX = if (offsetX > 0) offsetX - viewWidth else offsetX + viewWidth
        if (mDirection == Direction.PREV) {
            bitmapMatrix.setTranslate(distanceX, 0.toFloat())
            curBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
            prevBitmap?.let { canvas.drawBitmap(it, bitmapMatrix, null) }
            addShadow(distanceX.toInt(), canvas)
        } else if (mDirection == Direction.NEXT) {
            bitmapMatrix.setTranslate(distanceX - viewWidth, 0.toFloat())
            nextBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
            curBitmap?.let { canvas.drawBitmap(it, bitmapMatrix, null) }
            addShadow(distanceX.toInt(), canvas)
        }
    }

    private fun addShadow(left: Int, canvas: Canvas) {
        if (left < 0) {
            shadowDrawableR.setBounds(left + viewWidth, 0, left + viewWidth + 30, viewHeight)
            shadowDrawableR.draw(canvas)
        } else if (left > 0) {
            shadowDrawableR.setBounds(left, 0, left + 30, viewHeight)
            shadowDrawableR.draw(canvas)
        }
    }

    override fun onAnimStop() {
        if (!isCancel) {
            pageView.fillPage(mDirection)
        }
    }

    override fun onAnimStart(animationSpeed: Int) {
        val distanceX: Float
        when (mDirection) {
            Direction.NEXT -> distanceX =
                if (isCancel) {
                    var dis = viewWidth - startX + touchX
                    if (dis > viewWidth) {
                        dis = viewWidth.toFloat()
                    }
                    viewWidth - dis
                } else {
                    -(touchX + (viewWidth - startX))
                }
            else -> distanceX =
                if (isCancel) {
                    -(touchX - startX)
                } else {
                    viewWidth - (touchX - startX)
                }
        }
        startScroll(touchX.toInt(), 0, distanceX.toInt(), 0, animationSpeed)
    }

}
