package io.legado.app.ui.book.read.page.delegate

import android.graphics.Canvas
import io.legado.app.ui.book.read.page.PageView

class SlidePageDelegate(pageView: PageView) : HorizontalPageDelegate(pageView) {

    override fun setStartPoint(x: Float, y: Float, invalidate: Boolean) {
        curPage.x = 0f
        prevPage.x = -viewWidth.toFloat()
        nextPage.x = viewWidth.toFloat()
        super.setStartPoint(x, y, invalidate)
    }

    override fun onAnimStart() {
        val distanceX: Float
        when (mDirection) {
            Direction.NEXT -> distanceX =
                if (isCancel) {
                    var dis = viewWidth - startX + touchX
                    if (dis > viewWidth) {
                        dis = viewWidth.toFloat()
                    }
                    viewWidth - dis
                } else {
                    -(touchX + (viewWidth - startX))
                }
            else -> distanceX =
                if (isCancel) {
                    -(touchX - startX)
                } else {
                    viewWidth - (touchX - startX)
                }
        }
        startScroll(touchX.toInt(), 0, distanceX.toInt(), 0)
    }

    override fun onDraw(canvas: Canvas) {
        val offsetX = touchX - startX

        if ((mDirection == Direction.NEXT && offsetX > 0)
            || (mDirection == Direction.PREV && offsetX < 0)
        ) return

        if (!isMoved) return
        if (mDirection == Direction.PREV) {
            curPage.translationX = offsetX
            prevPage.translationX = offsetX - viewWidth
        } else if (mDirection == Direction.NEXT) {
            curPage.translationX = offsetX
            nextPage.translationX = offsetX + viewWidth
        }
    }

    override fun onAnimStop() {
        curPage.x = 0f
        prevPage.x = -viewWidth.toFloat()
        nextPage.x = viewWidth.toFloat()
        if (!isCancel) {
            pageView.fillPage(mDirection)
        }
    }
}