package io.legado.app.ui.book.read.page.delegate

import android.view.MotionEvent
import io.legado.app.ui.book.read.page.PageView
import kotlin.math.abs

abstract class HorizontalPageDelegate(pageView: PageView) : PageDelegate(pageView) {

    override fun onTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                abort()
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
        //判断是否移动了
        if (!isMoved) {
            isMoved = abs(startX - event.x) > slop
                    || abs(startX - event.x) > abs(startY - event.y)
            if (isMoved) {
                if (event.x - startX > 0) {
                    //如果上一页不存在
                    if (!hasPrev()) {
                        noNext = true
                        return
                    }
                    setDirection(Direction.PREV)
                } else {
                    //如果不存在表示没有下一页了
                    if (!hasNext()) {
                        noNext = true
                        return
                    }
                    setDirection(Direction.NEXT)
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

    override fun nextPageByAnim() {
        super.nextPageByAnim()
        if (!hasNext()) return
        setDirection(Direction.NEXT)
        setTouchPoint(viewWidth.toFloat(), 0f)
        onAnimStart()
    }

    override fun prevPageByAnim() {
        super.prevPageByAnim()
        if (!hasPrev()) return
        setDirection(Direction.PREV)
        setTouchPoint(0f, 0f)
        onAnimStart()
    }

}