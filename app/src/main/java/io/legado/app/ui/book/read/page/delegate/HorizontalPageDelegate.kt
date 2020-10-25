package io.legado.app.ui.book.read.page.delegate

import android.graphics.Bitmap
import android.view.MotionEvent
import io.legado.app.ui.book.read.page.PageView
import io.legado.app.utils.screenshot

abstract class HorizontalPageDelegate(pageView: PageView) : PageDelegate(pageView) {

    protected var curBitmap: Bitmap? = null
    protected var prevBitmap: Bitmap? = null
    protected var nextBitmap: Bitmap? = null

    override fun setDirection(direction: Direction) {
        super.setDirection(direction)
        setBitmap()
    }

    private fun setBitmap() {
        when (mDirection) {
            Direction.PREV -> {
                prevBitmap?.recycle()
                prevBitmap = prevPage.screenshot()
                curBitmap?.recycle()
                curBitmap = curPage.screenshot()
            }
            Direction.NEXT -> {
                nextBitmap?.recycle()
                nextBitmap = nextPage.screenshot()
                curBitmap?.recycle()
                curBitmap = curPage.screenshot()
            }
            else -> Unit
        }
    }

    override fun onTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                abortAnim()
            }
            MotionEvent.ACTION_MOVE -> {
                onScroll(event)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                onAnimStart(pageView.defaultAnimationSpeed)
            }
        }
    }

    private fun onScroll(event: MotionEvent) {

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
        //判断是否移动了
        if (!isMoved) {
            val deltaX = (focusX - startX).toInt()
            val deltaY = (focusY - startY).toInt()
            val distance = deltaX * deltaX + deltaY * deltaY
            isMoved = distance > pageView.slopSquare
            if (isMoved) {
                if (sumX - startX > 0) {
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
            isCancel = if (mDirection == Direction.NEXT) sumX > lastX else sumX < lastX
            isRunning = true
            //设置触摸点
            pageView.setTouchPoint(sumX, sumY)
        }
    }

    override fun abortAnim() {
        isStarted = false
        isMoved = false
        isRunning = false
        if (!scroller.isFinished) {
            pageView.isAbortAnim = true
            scroller.abortAnimation()
            if (!isCancel) {
                pageView.fillPage(mDirection)
                pageView.invalidate()
            }
        } else {
            pageView.isAbortAnim = false
        }
    }

    override fun nextPageByAnim(animationSpeed: Int) {
        abortAnim()
        if (!hasNext()) return
        setDirection(Direction.NEXT)
        pageView.setTouchPoint(viewWidth.toFloat(), 0f, false)
        onAnimStart(animationSpeed)
    }

    override fun prevPageByAnim(animationSpeed: Int) {
        abortAnim()
        if (!hasPrev()) return
        setDirection(Direction.PREV)
        pageView.setTouchPoint(0f, 0f)
        onAnimStart(animationSpeed)
    }

    override fun onDestroy() {
        super.onDestroy()
        prevBitmap?.recycle()
        prevBitmap = null
        curBitmap?.recycle()
        curBitmap = null
        nextBitmap?.recycle()
        nextBitmap = null
    }

}