package io.legado.app.ui.widget.page.delegate

import android.graphics.Canvas
import android.view.MotionEvent
import io.legado.app.ui.widget.page.PageView
import io.legado.app.ui.widget.page.curl.CurlPage
import io.legado.app.ui.widget.page.curl.CurlView
import io.legado.app.utils.screenshot

class SimulationPageDelegate(pageView: PageView) : HorizontalPageDelegate(pageView) {

    init {
        pageView.curlView ?: let {
            pageView.curlView = CurlView(pageView.context)
            pageView.addView(pageView.curlView)
            pageView.curlView?.mPageProvider = PageProvider()
            pageView.curlView?.setSizeChangedObserver(SizeChangedObserver())
            pageView.curlView?.currentIndex = 1
        }
    }

    override fun onTouch(event: MotionEvent): Boolean {
        pageView.curlView?.dispatchTouchEvent(event)
        return super.onTouch(event)
    }

    override fun onScrollStart() {
    }

    override fun onDraw(canvas: Canvas) {
    }

    override fun onScrollStop() {
    }

    override fun onPageUp() {
        pageView.curlView?.updatePages()
        pageView.curlView?.requestRender()
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
            pageView.curlView?.setViewMode(CurlView.SHOW_ONE_PAGE)
            pageView.curlView?.setMargins(0f, 0f, 0f, 0f)
        }
    }
}