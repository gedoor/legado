package io.legado.app.ui.book.read.page.delegate

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Picture
import android.os.Build
import androidx.core.graphics.withTranslation
import io.legado.app.ui.book.read.page.ReadView
import io.legado.app.ui.book.read.page.entities.PageDirection
import io.legado.app.utils.screenshot

class SlidePageDelegate(readView: ReadView) : HorizontalPageDelegate(readView) {

    private val bitmapMatrix = Matrix()

    private lateinit var curPicture: Picture
    private lateinit var prevPicture: Picture
    private lateinit var nextPicture: Picture

    private val atLeastApi23 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    init {
        if (atLeastApi23) {
            curPicture = Picture()
            prevPicture = Picture()
            nextPicture = Picture()
        }
    }

    override fun setBitmap() {
        if (!atLeastApi23) {
            return super.setBitmap()
        }
        when (mDirection) {
            PageDirection.PREV -> {
                prevPage.screenshot(prevPicture)
                curPage.screenshot(curPicture)
            }

            PageDirection.NEXT -> {
                nextPage.screenshot(nextPicture)
                curPage.screenshot(curPicture)
            }

            else -> Unit
        }
    }

    override fun onAnimStart(animationSpeed: Int) {
        val distanceX: Float
        when (mDirection) {
            PageDirection.NEXT -> distanceX =
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

    override fun onDraw(canvas: Canvas) {
        val offsetX = touchX - startX

        if ((mDirection == PageDirection.NEXT && offsetX > 0)
            || (mDirection == PageDirection.PREV && offsetX < 0)
        ) return
        val distanceX = if (offsetX > 0) offsetX - viewWidth else offsetX + viewWidth
        if (!isRunning) return
        if (mDirection == PageDirection.PREV) {
            if (!atLeastApi23) {
                bitmapMatrix.setTranslate(distanceX + viewWidth, 0.toFloat())
                curBitmap?.let { canvas.drawBitmap(it, bitmapMatrix, null) }
                bitmapMatrix.setTranslate(distanceX, 0.toFloat())
                prevBitmap?.let { canvas.drawBitmap(it, bitmapMatrix, null) }
            } else {
                canvas.withTranslation(distanceX + viewWidth) {
                    drawPicture(curPicture)
                }
                canvas.withTranslation(distanceX) {
                    drawPicture(prevPicture)
                }
            }
        } else if (mDirection == PageDirection.NEXT) {
            if (!atLeastApi23) {
                bitmapMatrix.setTranslate(distanceX, 0.toFloat())
                nextBitmap?.let { canvas.drawBitmap(it, bitmapMatrix, null) }
                bitmapMatrix.setTranslate(distanceX - viewWidth, 0.toFloat())
                curBitmap?.let { canvas.drawBitmap(it, bitmapMatrix, null) }
            } else {
                canvas.withTranslation(distanceX) {
                    drawPicture(nextPicture)
                }
                canvas.withTranslation(distanceX - viewWidth) {
                    drawPicture(curPicture)
                }
            }
        }
    }

    override fun onAnimStop() {
        if (!isCancel) {
            readView.fillPage(mDirection)
        }
    }
}