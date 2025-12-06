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
        private set
    private var isPausing = false
    private var scrollOffsetRemain = 0.0
    private var scrollOffset = 0
    private var lastTimeMillis = 0L
    private var canvasRecorder = CanvasRecorderFactory.create()
    private val paint by lazy { Paint() }

    // 墨水屏模式
    private var eInkStep = 0
    private val eInkTotalSteps = 4
    private var eInkLastStepTime = 0L

    fun start() {
        isRunning = true
        paint.color = ThemeStore.accentColor
        lastTimeMillis = SystemClock.uptimeMillis()
        readView.curPage.upSelectAble(false)
        readView.invalidate()

        if (AppConfig.isEInkMode) {
            eInkStep = 0
            eInkLastStepTime = lastTimeMillis
        }
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
        if (AppConfig.isEInkMode) {
            eInkLastStepTime = lastTimeMillis
        }
        readView.invalidate()
    }

    fun reset() {
        progress = 0
        scrollOffsetRemain = 0.0
        scrollOffset = 0
        canvasRecorder.invalidate()
        eInkStep = 0
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
            if (!isPausing) {
                readView.curPage.scroll(-scrollOffset)
                scrollOffset = 0
            }
        } else {
            val width = readView.width
            val height = readView.height

            if (AppConfig.isEInkMode) {
                // 墨水屏模式分步绘制
                if (eInkStep > 0) {
                    canvasRecorder.recordIfNeeded(readView.nextPage)
                    val stepHeight = height * eInkStep / eInkTotalSteps
                    canvas.withClip(0, 0, width, stepHeight) {
                        canvasRecorder.draw(this)
                    }
                    canvas.drawRect(
                        0f,
                        stepHeight.toFloat() - 1,
                        width.toFloat(),
                        stepHeight.toFloat(),
                        paint
                    )
                }
                if (eInkStep < eInkTotalSteps && !isPausing) {
                    readView.postInvalidate()
                }
            } else {
                // 非墨水屏持续绘制
                val bottom = progress

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
    }

    fun computeOffset() {
        if (!isRunning || isPausing) {
            return
        }

        // 墨水屏模式计时翻页
        if (AppConfig.isEInkMode) {
            val currentTime = SystemClock.uptimeMillis()
            val stepInterval = (ReadBookConfig.autoReadSpeed * 1000 / eInkTotalSteps).toLong()

            if (currentTime - eInkLastStepTime >= stepInterval) {
                eInkStep++
                eInkLastStepTime = currentTime

                if (eInkStep >= eInkTotalSteps) {
                    if (!readView.fillPage(PageDirection.NEXT)) {
                        stop()
                    } else {
                        reset()
                    }
                }
            }
            return
        }

        // 非墨水屏按帧翻页
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