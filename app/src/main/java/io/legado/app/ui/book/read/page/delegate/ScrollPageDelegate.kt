package io.legado.app.ui.book.read.page.delegate

import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import io.legado.app.ui.book.read.page.PageView
import kotlin.math.abs

class ScrollPageDelegate(pageView: PageView) : PageDelegate(pageView) {

    // 滑动追踪的时间
    private val velocityDuration = 1000
    //速度追踪器
    private val mVelocity: VelocityTracker = VelocityTracker.obtain()
    private val slop = ViewConfiguration.get(pageView.context).scaledTouchSlop

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

    override fun onTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                abort()
                mVelocity.clear()
            }
            MotionEvent.ACTION_MOVE -> {
                mVelocity.addMovement(event)
                mVelocity.computeCurrentVelocity(velocityDuration)
                setTouchPoint(event.x, event.y)
                if (!isMoved) {
                    isMoved = abs(startY - event.y) > slop
                }
                if (isMoved) {
                    isRunning = true
                }
            }
        }
        return super.onTouch(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        mVelocity.recycle()
    }
}