package io.legado.app.ui.book.read.page.delegate

import android.view.MotionEvent
import android.view.VelocityTracker
import io.legado.app.ui.book.read.page.PageView
import kotlin.math.abs

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
        setTouchPoint(event.x, event.y)
        if (!isMoved) {
            isMoved = abs(startX - event.x) > slop || abs(startY - event.y) > slop
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

    }

    override fun prevPageByAnim() {

    }
}