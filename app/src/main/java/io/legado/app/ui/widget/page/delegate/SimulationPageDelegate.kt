package io.legado.app.ui.widget.page.delegate

import android.graphics.Canvas
import android.view.MotionEvent
import io.legado.app.ui.widget.page.PageView
import io.legado.app.ui.widget.page.curl.CurlPage
import io.legado.app.ui.widget.page.curl.CurlView
import io.legado.app.utils.screenshot

class SimulationPageDelegate(pageView: PageView) : PageDelegate(pageView) {

    init {
        pageView.curlView ?: let {
            pageView.curlView = CurlView(pageView.context)
            pageView.addView(pageView.curlView)
            pageView.curlView?.setPageProvider(PageProvider())
            pageView.curlView?.setSizeChangedObserver(SizeChangedObserver())
            pageView.curlView?.currentIndex = 0
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

    private inner class PageProvider : CurlView.PageProvider {

        override val pageCount: Int
            get() = 1

        override fun updatePage(page: CurlPage, width: Int, height: Int, index: Int) {
            val front = curPage?.screenshot()
            page.setTexture(front, CurlPage.SIDE_BOTH)
        }
    }

    // 定义书籍尺寸的变化监听器
    private inner class SizeChangedObserver : CurlView.SizeChangedObserver {
        override fun onSizeChanged(w: Int, h: Int) {
            pageView.curlView?.setViewMode(CurlView.SHOW_ONE_PAGE)
            pageView.curlView?.setMargins(0f, 0f, 0f, 0f)
        }
    }
}