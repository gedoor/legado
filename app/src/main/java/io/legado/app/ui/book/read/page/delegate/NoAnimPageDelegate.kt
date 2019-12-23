package io.legado.app.ui.book.read.page.delegate

import android.graphics.Canvas
import io.legado.app.ui.book.read.page.PageView

class NoAnimPageDelegate(pageView: PageView) : HorizontalPageDelegate(pageView) {
    override fun onScrollStart() {
        startScroll(touchX.toInt(), 0, 0, 0)
    }

    override fun onDraw(canvas: Canvas) {
    }

    override fun onScrollStop() {
        if (!isCancel) {
            pageView.fillPage(direction)
        }
    }
}