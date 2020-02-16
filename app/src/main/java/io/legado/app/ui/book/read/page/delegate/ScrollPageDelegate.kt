package io.legado.app.ui.book.read.page.delegate

import android.graphics.Canvas
import android.graphics.Matrix
import android.text.method.ScrollingMovementMethod
import android.view.MotionEvent
import io.legado.app.constant.PreferKey
import io.legado.app.ui.book.read.page.PageView
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.screenshot
import kotlin.math.abs

class ScrollPageDelegate(pageView: PageView) : PageDelegate(pageView) {

    private val bitmapMatrix = Matrix()

    override fun onScrollStart() {
        if (!atTop && !atBottom) {
            stopScroll()
            return
        }
        val distanceY: Float
        when (direction) {
            Direction.NEXT -> distanceY =
                if (isCancel) {
                    var dis = viewHeight - startY + touchY
                    if (dis > viewHeight) {
                        dis = viewHeight.toFloat()
                    }
                    viewHeight - dis
                } else {
                    -(touchY + (viewHeight - startY))
                }
            else -> distanceY =
                if (isCancel) {
                    -(touchY - startY)
                } else {
                    viewHeight - (touchY - startY)
                }
        }

        startScroll(0, touchY.toInt(), 0, distanceY.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        if (atTop || atBottom) {
            val offsetY = touchY - startY

            if ((direction == Direction.NEXT && offsetY > 0)
                || (direction == Direction.PREV && offsetY < 0)
            ) return

            val distanceY = if (offsetY > 0) offsetY - viewHeight else offsetY + viewHeight
            if (atTop && direction == Direction.PREV) {
                bitmap?.let {
                    bitmapMatrix.setTranslate(0.toFloat(), distanceY)
                    canvas.drawBitmap(it, bitmapMatrix, null)
                }
            } else if (atBottom && direction == Direction.NEXT) {
                bitmap?.let {
                    bitmapMatrix.setTranslate(0.toFloat(), distanceY)
                    canvas.drawBitmap(it, bitmapMatrix, null)
                }
            }
        }
    }

    override fun onScrollStop() {
        if (!isCancel) {
            pageView.fillPage(direction)
        }
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (!isMoved && abs(distanceX) < abs(distanceY)) {
            if (distanceY < 0) {
                if (atTop) {
                    val event = e1.toAction(MotionEvent.ACTION_UP)
                    curPage.dispatchTouchEvent(event)
                    event.recycle()
                    //如果上一页不存在
                    if (!hasPrev()) {
                        noNext = true
                        return true
                    }
                    //上一页截图
                    bitmap = prevPage.screenshot()
                }
            } else {
                if (atBottom) {
                    val event = e1.toAction(MotionEvent.ACTION_UP)
                    curPage.dispatchTouchEvent(event)
                    event.recycle()
                    //如果不存在表示没有下一页了
                    if (!hasNext()) {
                        noNext = true
                        return true
                    }
                    //下一页截图
                    bitmap = nextPage.screenshot()
                }
            }
            isMoved = true
        }
        if ((atTop && direction != Direction.PREV) || (atBottom && direction != Direction.NEXT) || direction == Direction.NONE) {
            //传递触摸事件到textView
            curPage.dispatchTouchEvent(e2)
        }
        if (isMoved) {
            isCancel = if (direction == Direction.NEXT) distanceY < 0 else distanceY > 0
            isRunning = true
            //设置触摸点
            setTouchPoint(e2.x, e2.y)
        }
        return isMoved
    }

    override fun upSelectAble() {
        pageView.curPage.contentTextView()?.apply {
            if (context.getPrefBoolean(PreferKey.selectText)) {
                setTextIsSelectable(true)
            } else {
                setTextIsSelectable(false)
                movementMethod = ScrollingMovementMethod.getInstance()
            }
        }
    }
}