package io.legado.app.ui.book.read.page.delegate

import android.view.MotionEvent
import io.legado.app.ui.book.read.page.PageView

class NoAnimPageDelegate(pageView: PageView) : HorizontalPageDelegate(pageView) {

    override fun onAnimStart(animationSpeed: Int) {
        if (!isCancel) {
            pageView.fillPage(mDirection)
        }
        stopScroll()
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        return onSingleTapUp(e)
    }

}