package io.legado.app.ui.widget.page

import android.widget.Scroller
import androidx.interpolator.view.animation.FastOutLinearInInterpolator

abstract class PageAnimDelegate(protected val pageView: PageView) {

    protected val scroller: Scroller = Scroller(pageView.context, FastOutLinearInInterpolator())

    //起始点
    protected var startX: Float = 0.toFloat()
    protected var startY: Float = 0.toFloat()
    //触碰点
    protected var touchX: Float = 0.toFloat()
    protected var touchY: Float = 0.toFloat()
    //上一个触碰点
    protected var lastX: Float = 0.toFloat()
    protected var lastY: Float = 0.toFloat()

    protected var isRunning = false
    protected var isStarted = false


    fun setStartPoint(x: Float, y: Float) {
        startX = x
        startY = y

        lastX = startX
        lastY = startY
    }

    fun setTouchPoint(x: Float, y: Float) {
        lastX = touchX
        lastY = touchY

        touchX = x
        touchY = y
    }
}