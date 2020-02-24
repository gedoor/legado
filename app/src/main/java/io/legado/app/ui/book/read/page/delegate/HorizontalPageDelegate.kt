package io.legado.app.ui.book.read.page.delegate

import android.view.MotionEvent
import io.legado.app.ui.book.read.page.PageView
import kotlin.math.abs

abstract class HorizontalPageDelegate(pageView: PageView) : PageDelegate(pageView) {

    override fun onTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                //判断是否移动了
                if (!isMoved) {
                    isMoved = abs(startX - event.x) > slop || abs(startY - event.y) > slop
                    if (isMoved) {
                        if (event.x - startX > 0) {
                            //如果上一页不存在
                            if (!hasPrev()) {
                                noNext = true
                                return
                            }
                            setDirection(Direction.PREV)
                            setBitmap()
                        } else {
                            //如果不存在表示没有下一页了
                            if (!hasNext()) {
                                noNext = true
                                return
                            }
                            setDirection(Direction.NEXT)
                            setBitmap()
                        }
                    }
                }
                if (isMoved) {
                    isCancel = if (mDirection == Direction.NEXT) touchX > lastX else touchX < lastX
                    isRunning = true
                    //设置触摸点
                    setTouchPoint(event.x, event.y)
                }
            }
        }
        super.onTouch(event)
    }

}