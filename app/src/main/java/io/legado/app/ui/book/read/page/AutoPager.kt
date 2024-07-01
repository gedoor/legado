package io.legado.app.ui.book.read.page

import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import androidx.core.graphics.withClip
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.ui.book.read.page.entities.PageDirection
import io.legado.app.utils.canvasrecorder.CanvasRecorderFactory
import io.legado.app.utils.canvasrecorder.recordIfNeeded

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
    private var canvasRecorder = CanvasRecorderFactory.create()
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
        canvasRecorder.recycle()
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
        canvasRecorder.invalidate()
    }

    fun upRecorder() {
        canvasRecorder.recycle()
        canvasRecorder = CanvasRecorderFactory.create()
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

            canvasRecorder.recordIfNeeded(readView.nextPage)
            canvas.withClip(0, 0, width, bottom) {
                canvasRecorder.draw(this)
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
