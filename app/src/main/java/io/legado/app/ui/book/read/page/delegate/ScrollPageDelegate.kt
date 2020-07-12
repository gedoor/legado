package io.legado.app.ui.book.read.page.delegate

import android.view.MotionEvent
import android.view.VelocityTracker
import io.legado.app.ui.book.read.page.PageView
import io.legado.app.ui.book.read.page.provider.ChapterProvider

class ScrollPageDelegate(pageView: PageView) : PageDelegate(pageView) {

    // 滑动追踪的时间
    private val velocityDuration = 1000
    //速度追踪器
    private val mVelocity: VelocityTracker = VelocityTracker.obtain()

    override fun onAnimStart() {
        //惯性滚动
        fling(
            0, touchY.toInt(), 0, mVelocity.yVelocity.toInt(),
            0, 0, -10 * viewHeight, 10 * viewHeight
        )
    }

    override fun onScroll() {
        curPage.onScroll(touchY - lastY)
    }

    override fun onTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                setStartPoint(event.x, event.y)
                abort()
                mVelocity.clear()
            }
            MotionEvent.ACTION_MOVE -> {
                if (isTextSelected) {
                    selectText(event)
                } else {
                    onScroll(event)
                }
            }
        }
        super.onTouch(event)
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
        setTouchPoint(sumX, sumY)
        if (!isMoved) {
            val deltaX = (focusX - startX).toInt()
            val deltaY = (focusY - startY).toInt()
            val distance = deltaX * deltaX + deltaY * deltaY
            isMoved = distance > slopSquare
        }
        if (isMoved) {
            isRunning = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mVelocity.recycle()
    }

    override fun nextPageByAnim() {
        abort()
        startScroll(0, 0, 0, -ChapterProvider.visibleHeight)
    }

    override fun prevPageByAnim() {
        abort()
        startScroll(0, 0, 0, ChapterProvider.visibleHeight)
    }
}