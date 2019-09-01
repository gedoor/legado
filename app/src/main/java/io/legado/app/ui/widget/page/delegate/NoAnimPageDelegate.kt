package io.legado.app.ui.widget.page.delegate

import android.graphics.Canvas
import io.legado.app.ui.widget.page.PageView

class NoAnimPageDelegate(pageView: PageView) : PageDelegate(pageView) {
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