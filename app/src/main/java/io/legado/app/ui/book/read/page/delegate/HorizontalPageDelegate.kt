package io.legado.app.ui.book.read.page.delegate

import android.view.MotionEvent
import io.legado.app.ui.book.read.page.PageView
import kotlin.math.abs

abstract class HorizontalPageDelegate(pageView: PageView) : PageDelegate(pageView) {

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (!isMoved) {
            if (abs(distanceX) > abs(distanceY)) {
                if (distanceX < 0) {
                    //如果上一页不存在
                    if (!hasPrev()) {
                        noNext = true
                        return true
                    }
                    setDirection(Direction.PREV)
                    setBitmap()
                } else {
                    //如果不存在表示没有下一页了
                    if (!hasNext()) {
                        noNext = true
                        return true
                    }
                    setDirection(Direction.NEXT)
                    setBitmap()
                }
                isMoved = true
            }
        }
        if (isMoved) {
            isCancel = if (mDirection == Direction.NEXT) distanceX < 0 else distanceX > 0
            isRunning = true
            //设置触摸点
            setTouchPoint(e2.x, e2.y)
        }
        return isMoved
    }

}