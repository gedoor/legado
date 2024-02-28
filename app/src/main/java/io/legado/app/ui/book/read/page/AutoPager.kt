package io.legado.app.ui.book.read.page

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import androidx.core.graphics.withClip
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.ui.book.read.page.entities.PageDirection
import io.legado.app.utils.screenshot

/**
 * 自动翻页
 */
class AutoPager(private val readView: ReadView) {
    private var progress = 0
    var isRunning = false
    private var isPausing = false
    private var scrollOffsetRemain = 0.0
    private var scrollOffset = 0
    private var lastTimeMillis = 0L
    private var bitmap: Bitmap? = null
    private var picture: Picture? = null
    private var pictureIsDirty = true
    private val atLeastApi23 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    private val rect = Rect()
    private val paint by lazy { Paint() }

    fun start() {
        isRunning = true
        paint.color = ThemeStore.accentColor
        lastTimeMillis = SystemClock.uptimeMillis()
        readView.curPage.upSelectAble(false)
        readView.invalidate()
    }

    fun stop() {
        if (!isRunning) {
            return
        }
        isRunning = false
        isPausing = false
        readView.curPage.upSelectAble(AppConfig.textSelectAble)
        readView.invalidate()
        reset()
        picture = null
    }

    fun pause() {
        if (!isRunning) {
            return
        }
        isPausing = true
    }

    fun resume() {
        if (!isRunning) {
            return
        }
        isPausing = false
        lastTimeMillis = SystemClock.uptimeMillis()
        readView.invalidate()
    }

    fun reset() {
        progress = 0
        scrollOffsetRemain = 0.0
        scrollOffset = 0
        bitmap?.recycle()
        bitmap = null
        pictureIsDirty = true
    }

    fun onDraw(canvas: Canvas) {
        if (!isRunning) {
            return
        }

        if (readView.isScroll) {
            if (!isPausing) readView.curPage.scroll(-scrollOffset)
        } else {
            val bottom = progress
            val width = readView.width
            if (atLeastApi23) {
                if (picture == null) {
                    picture = Picture()
                }
                if (pictureIsDirty) {
                    pictureIsDirty = false
                    readView.nextPage.screenshot(picture!!)
                }
                canvas.withClip(0, 0, width, bottom) {
                    drawPicture(picture!!)
                }
            } else {
                if (bitmap == null) {
                    bitmap = readView.nextPage.screenshot()
                }
                rect.set(0, 0, width, bottom)
                canvas.drawBitmap(bitmap!!, rect, rect, null)
            }
            canvas.drawRect(
                0f,
                bottom.toFloat() - 1,
                width.toFloat(),
                bottom.toFloat(),
                paint
            )
            if (!isPausing) readView.postInvalidate()
        }

    }

    fun computeOffset() {
        if (!isRunning) {
            return
        }

        val currentTime = SystemClock.uptimeMillis()
        val elapsedTime = currentTime - lastTimeMillis
        lastTimeMillis = currentTime

        val readTime = ReadBookConfig.autoReadSpeed * 1000.0
        val height = readView.height
        scrollOffsetRemain += height / readTime * elapsedTime
        if (scrollOffsetRemain < 1) {
            return
        }
        scrollOffset = scrollOffsetRemain.toInt()
        this.scrollOffsetRemain -= scrollOffset
        if (!readView.isScroll) {
            progress += scrollOffset
            if (progress >= height) {
                if (!readView.fillPage(PageDirection.NEXT)) {
                    stop()
                } else {
                    reset()
                }
            }
        }
    }

}
