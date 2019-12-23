package io.legado.app.ui.book.read.page.delegate

import android.graphics.Canvas
import android.view.MotionEvent
import io.legado.app.ui.book.read.page.PageView
import io.legado.app.ui.book.read.page.curl.CurlPage
import io.legado.app.ui.book.read.page.curl.CurlView
import io.legado.app.utils.screenshot
import kotlin.math.abs

class SimulationPageDelegate(pageView: PageView) : HorizontalPageDelegate(pageView),
    CurlView.CallBack {

    var curlView: CurlView? = null

    init {
        pageView.curlView ?: let {
            curlView = CurlView(pageView.context)
            pageView.curlView = curlView
            pageView.addView(curlView)
            curlView?.mPageProvider = PageProvider()
            curlView?.setSizeChangedObserver(SizeChangedObserver())
            curlView?.callBack = this
        }
    }

    override fun onTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                curlView?.currentIndex = 1
            }
        }
        curlView?.dispatchTouchEvent(event)
        return super.onTouch(event)
    }

    override fun onScrollStart() {
    }

    override fun onDraw(canvas: Canvas) {
    }

    override fun onScrollStop() {
    }

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
            curlView?.canDraw = true
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

    override fun onPageUp() {
        curlView?.updatePages()
        curlView?.requestRender()
    }

    override fun pageChange(change: Int) {
        pageView.post {
            if (change > 0) {
                pageView.moveToNextPage()
            } else {
                pageView.moveToPrevPage()
            }
        }
    }

    private inner class PageProvider : CurlView.PageProvider {

        override val pageCount: Int
            get() = 3

        override fun updatePage(page: CurlPage, width: Int, height: Int, index: Int) {
            when (index) {
                0 -> page.setTexture(prevPage?.screenshot(), CurlPage.SIDE_BOTH)
                1 -> page.setTexture(curPage?.screenshot(), CurlPage.SIDE_BOTH)
                2 -> page.setTexture(nextPage?.screenshot(), CurlPage.SIDE_BOTH)
            }
        }
    }

    // 定义书籍尺寸的变化监听器
    private inner class SizeChangedObserver : CurlView.SizeChangedObserver {
        override fun onSizeChanged(width: Int, height: Int) {
            curlView?.setViewMode(CurlView.SHOW_ONE_PAGE)
        }
    }
}