package io.legado.app.ui.widget.page.delegate

import android.view.MotionEvent
import io.legado.app.ui.widget.page.PageView
import io.legado.app.utils.screenshot
import kotlin.math.abs

abstract class HorizontalPageDelegate(pageView: PageView) : PageDelegate(pageView) {

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (!isMoved) {
            val event = e1.toAction(MotionEvent.ACTION_UP)
            curPage?.dispatchTouchEvent(event)
            event.recycle()
            if (abs(distanceX) > abs(distanceY)) {
                if (distanceX < 0) {
                    //如果上一页不存在
                    if (!hasPrev()) {
                        noNext = true
                        return true
                    }
                    //上一页截图
                    bitmap = prevPage?.screenshot()
                } else {
                    //如果不存在表示没有下一页了
                    if (!hasNext()) {
                        noNext = true
                        return true
                    }
                    //下一页截图
                    bitmap = nextPage?.screenshot()
                }
                isMoved = true
            }
        }
        if (isMoved) {
            isCancel = if (pageView.isScrollDelegate) {
                if (direction == Direction.NEXT) distanceY < 0 else distanceY > 0
            } else {
                if (direction == Direction.NEXT) distanceX < 0 else distanceX > 0
            }
            isRunning = true
            //设置触摸点
            setTouchPoint(e2.x, e2.y)
        }
        return isMoved
    }
}