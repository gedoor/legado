package io.legado.app.ui.book.read.page.delegate

import android.view.MotionEvent
import io.legado.app.ui.book.read.page.ReadView
import io.legado.app.ui.book.read.page.entities.PageDirection
import io.legado.app.utils.canvasrecorder.CanvasRecorderFactory
import io.legado.app.utils.screenshot

abstract class HorizontalPageDelegate(readView: ReadView) : PageDelegate(readView) {

    protected var curRecorder = CanvasRecorderFactory.create()
    protected var prevRecorder = CanvasRecorderFactory.create()
    protected var nextRecorder = CanvasRecorderFactory.create()
    private val slopSquare get() = readView.pageSlopSquare2

    override fun setDirection(direction: PageDirection) {
        super.setDirection(direction)
        setBitmap()
    }

    open fun setBitmap() {
        when (mDirection) {
            PageDirection.PREV -> {
                prevPage.screenshot(prevRecorder)
                curPage.screenshot(curRecorder)
            }

            PageDirection.NEXT -> {
                nextPage.screenshot(nextRecorder)
                curPage.screenshot(curRecorder)
            }

            else -> Unit
        }
    }

    fun upRecorder() {
        curRecorder.recycle()
        prevRecorder.recycle()
        nextRecorder.recycle()
        curRecorder = CanvasRecorderFactory.create()
        prevRecorder = CanvasRecorderFactory.create()
        nextRecorder = CanvasRecorderFactory.create()
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
                onAnimStart(readView.defaultAnimationSpeed)
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
            isMoved = distance > slopSquare
            if (isMoved) {
                if (sumX - startX > 0) {
                    //如果上一页不存在
                    if (!hasPrev()) {
                        noNext = true
                        return
                    }
                    setDirection(PageDirection.PREV)
                } else {
                    //如果不存在表示没有下一页了
                    if (!hasNext()) {
                        noNext = true
                        return
                    }
                    setDirection(PageDirection.NEXT)
                }
                readView.setStartPoint(event.x, event.y, false)
            }
        }
        if (isMoved) {
            isCancel = if (mDirection == PageDirection.NEXT) sumX > lastX else sumX < lastX
            isRunning = true
            //设置触摸点
            readView.setTouchPoint(sumX, sumY)
        }
    }

    override fun abortAnim() {
        isStarted = false
        isMoved = false
        isRunning = false
        if (!scroller.isFinished) {
            readView.isAbortAnim = true
            scroller.abortAnimation()
            if (!isCancel) {
                readView.fillPage(mDirection)
                readView.invalidate()
            }
        } else {
            readView.isAbortAnim = false
        }
    }

    override fun nextPageByAnim(animationSpeed: Int) {
        abortAnim()
        if (!hasNext()) return
        setDirection(PageDirection.NEXT)
        val y = when {
            startY > viewHeight / 2 -> viewHeight.toFloat() * 0.9f
            else -> 1f
        }
        readView.setStartPoint(viewWidth.toFloat() * 0.9f, y, false)
        onAnimStart(animationSpeed)
    }

    override fun prevPageByAnim(animationSpeed: Int) {
        abortAnim()
        if (!hasPrev()) return
        setDirection(PageDirection.PREV)
        readView.setStartPoint(0f, viewHeight.toFloat(), false)
        onAnimStart(animationSpeed)
    }

    override fun onDestroy() {
        super.onDestroy()
        prevRecorder.recycle()
        curRecorder.recycle()
        nextRecorder.recycle()
    }

}