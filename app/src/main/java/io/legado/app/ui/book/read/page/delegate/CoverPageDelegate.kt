package io.legado.app.ui.book.read.page.delegate

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Picture
import android.graphics.drawable.GradientDrawable
import android.os.Build
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import io.legado.app.ui.book.read.page.ReadView
import io.legado.app.ui.book.read.page.entities.PageDirection
import io.legado.app.utils.screenshot

class CoverPageDelegate(readView: ReadView) : HorizontalPageDelegate(readView) {
    private val bitmapMatrix = Matrix()
    private val shadowDrawableR: GradientDrawable

    private lateinit var curPicture: Picture
    private lateinit var prevPicture: Picture
    private lateinit var nextPicture: Picture

    private val atLeastApi23 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    init {
        val shadowColors = intArrayOf(0x66111111, 0x00000000)
        shadowDrawableR = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, shadowColors
        )
        shadowDrawableR.gradientType = GradientDrawable.LINEAR_GRADIENT
        if (atLeastApi23) {
            curPicture = Picture()
            prevPicture = Picture()
            nextPicture = Picture()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!isRunning) return
        val offsetX = touchX - startX

        if ((mDirection == PageDirection.NEXT && offsetX > 0)
            || (mDirection == PageDirection.PREV && offsetX < 0)
        ) {
            return
        }

        val distanceX = if (offsetX > 0) offsetX - viewWidth else offsetX + viewWidth
        if (mDirection == PageDirection.PREV) {
            if (offsetX <= viewWidth) {
                if (!atLeastApi23) {
                    bitmapMatrix.setTranslate(distanceX, 0.toFloat())
                    prevBitmap?.let { canvas.drawBitmap(it, bitmapMatrix, null) }
                } else {
                    canvas.withTranslation(distanceX) {
                        drawPicture(prevPicture)
                    }
                }
                addShadow(distanceX, canvas)
            } else {
                if (!atLeastApi23) {
                    prevBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
                } else {
                    canvas.drawPicture(prevPicture)
                }
            }
        } else if (mDirection == PageDirection.NEXT) {
            if (!atLeastApi23) {
                bitmapMatrix.setTranslate(distanceX - viewWidth, 0.toFloat())
                nextBitmap?.let {
                    val width = it.width.toFloat()
                    val height = it.height.toFloat()
                    canvas.withClip(width + offsetX, 0f, width, height) {
                        drawBitmap(it, 0f, 0f, null)
                    }
                }
                curBitmap?.let { canvas.drawBitmap(it, bitmapMatrix, null) }
            } else {
                val width = nextPicture.width.toFloat()
                val height = nextPicture.height.toFloat()
                canvas.withClip(width + offsetX, 0f, width, height) {
                    drawPicture(nextPicture)
                }
                canvas.withTranslation(distanceX - viewWidth) {
                    drawPicture(curPicture)
                }
            }
            addShadow(distanceX, canvas)
        }
    }

    override fun setBitmap() {
        when (mDirection) {
            PageDirection.PREV -> if (!atLeastApi23) {
                prevBitmap = prevPage.screenshot(prevBitmap, canvas)
            } else {
                prevPage.screenshot(prevPicture)
            }

            PageDirection.NEXT -> if (!atLeastApi23) {
                nextBitmap = nextPage.screenshot(nextBitmap, canvas)
                curBitmap = curPage.screenshot(curBitmap, canvas)
            } else {
                nextPage.screenshot(nextPicture)
                curPage.screenshot(curPicture)
            }

            else -> Unit
        }
    }

    private fun addShadow(left: Float, canvas: Canvas) {
        if (left == 0f) return
        val dx = if (left < 0) {
            left + viewWidth
        } else {
            left
        }
        canvas.withTranslation(dx) {
            shadowDrawableR.draw(canvas)
        }
    }

    override fun setViewSize(width: Int, height: Int) {
        super.setViewSize(width, height)
        shadowDrawableR.setBounds(0, 0, 30, viewHeight)
    }

    override fun onAnimStop() {
        if (!isCancel) {
            readView.fillPage(mDirection)
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

}
