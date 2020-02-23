package io.legado.app.ui.book.read.page.delegate

import android.graphics.Canvas
import io.legado.app.ui.book.read.page.PageView

class NoAnimPageDelegate(pageView: PageView) : HorizontalPageDelegate(pageView) {
    override fun onAnimStart() {
        startScroll(touchX.toInt(), 0, 0, 0)
    }

    override fun onDraw(canvas: Canvas) {
    }

    override fun onAnimStop() {
        if (!isCancel) {
            pageView.fillPage(mDirection)
        }
    }
}