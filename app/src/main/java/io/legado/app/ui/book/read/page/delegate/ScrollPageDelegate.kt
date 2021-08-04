package io.legado.app.ui.book.read.page.delegate

import android.graphics.Canvas
import android.view.MotionEvent
import android.view.VelocityTracker
import io.legado.app.ui.book.read.page.ReadView
import io.legado.app.ui.book.read.page.provider.ChapterProvider

class ScrollPageDelegate(readView: ReadView) : PageDelegate(readView) {

    // 滑动追踪的时间
    private val velocityDuration = 1000

    //速度追踪器
    private val mVelocity: VelocityTracker = VelocityTracker.obtain()

    override fun onAnimStart(animationSpeed: Int) {
        //惯性滚动
        fling(
            0, touchY.toInt(), 0, mVelocity.yVelocity.toInt(),
            0, 0, -10 * viewHeight, 10 * viewHeight
        )
    }

    override fun onAnimStop() {
        // nothing
    }

    override fun onTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                abortAnim()
                mVelocity.clear()
            }
            MotionEvent.ACTION_MOVE -> {
                onScroll(event)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                onAnimStart(readView.defaultAnimationSpeed)
            }
        }
    }

    override fun onScroll() {
        curPage.scroll((touchY - lastY).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        // nothing
    }

    private fun onScroll(event: MotionEvent) {
        mVelocity.addMovement(event)
        mVelocity.computeCurrentVelocity(velocityDuration)
        val action: Int = event.action
        val pointerUp =
            action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_POINTER_UP
        val skipIndex = if (pointerUp) event.actionIndex else -1
        // Determine focal point
        var sumX = 0f
        var sumY = 0f
        val count: Int = event.pointerCount
        for (i in 0 until count) {
            if (skipIndex == i) continue
            sumX += event.getX(i)
            sumY += event.getY(i)
        }
        val div = if (pointerUp) count - 1 else count
        val focusX = sumX / div
        val focusY = sumY / div
        readView.setTouchPoint(sumX, sumY)
        if (!isMoved) {
            val deltaX = (focusX - startX).toInt()
            val deltaY = (focusY - startY).toInt()
            val distance = deltaX * deltaX + deltaY * deltaY
            isMoved = distance > readView.slopSquare
        }
        if (isMoved) {
            isRunning = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mVelocity.recycle()
    }

    override fun abortAnim() {
        isStarted = false
        isMoved = false
        isRunning = false
        if (!scroller.isFinished) {
            readView.isAbortAnim = true
            scroller.abortAnimation()
        } else {
            readView.isAbortAnim = false
        }
    }

    override fun nextPageByAnim(animationSpeed: Int) {
        if (readView.isAbortAnim) {
            return
        }
        readView.setStartPoint(0f, 0f, false)
        startScroll(0, 0, 0, -ChapterProvider.visibleHeight, animationSpeed)
    }

    override fun prevPageByAnim(animationSpeed: Int) {
        if (readView.isAbortAnim) {
            return
        }
        readView.setStartPoint(0f, 0f, false)
        startScroll(0, 0, 0, ChapterProvider.visibleHeight, animationSpeed)
    }
}