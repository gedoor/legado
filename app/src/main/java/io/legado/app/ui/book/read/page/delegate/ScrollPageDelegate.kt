package io.legado.app.ui.book.read.page.delegate

import android.graphics.Canvas
import android.view.MotionEvent
import android.view.VelocityTracker
import io.legado.app.ui.book.read.page.PageView

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

    override fun onDraw(canvas: Canvas) {

    }

    override fun onAnimStop() {
        if (!isCancel) {
            pageView.fillPage(mDirection)
        }
    }

    override fun onDown(e: MotionEvent): Boolean {
        mVelocity.clear()
        mVelocity.addMovement(e)
        return super.onDown(e)
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        mVelocity.addMovement(e2)
        mVelocity.computeCurrentVelocity(velocityDuration)
        setTouchPoint(e2.x, e2.y)

        curPage.onScroll(lastY - touchY)

        return true
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        mVelocity.addMovement(e2)
        return super.onFling(e1, e2, velocityX, velocityY)
    }

    override fun onDestroy() {
        super.onDestroy()
        mVelocity.recycle()
    }
}