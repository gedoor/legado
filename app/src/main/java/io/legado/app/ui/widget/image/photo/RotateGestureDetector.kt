package io.legado.app.ui.widget.image.photo

import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.atan

class RotateGestureDetector(private val mListener: OnRotateListener) {

    private val MAX_DEGREES_STEP = 120

    private var mPrevSlope = 0f
    private var mCurrSlope = 0f

    private val x1 = 0f
    private val y1 = 0f
    private val x2 = 0f
    private val y2 = 0f

    fun onTouchEvent(event: MotionEvent) {

        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount == 2) mPrevSlope = calculateSlope(event)
            }
            MotionEvent.ACTION_MOVE -> if (event.pointerCount > 1) {
                mCurrSlope = calculateSlope(event)

                val currDegrees = Math.toDegrees(atan(mCurrSlope.toDouble()))
                val prevDegrees = Math.toDegrees(atan(mPrevSlope.toDouble()))

                val deltaSlope = currDegrees - prevDegrees

                if (abs(deltaSlope) <= MAX_DEGREES_STEP) {
                    mListener.onRotate(deltaSlope.toFloat(), (x2 + x1) / 2, (y2 + y1) / 2)
                }
                mPrevSlope = mCurrSlope
            }
        }

    }

    private fun calculateSlope(event: MotionEvent): Float {
        val x1 = event.getX(0)
        val y1 = event.getY(0)
        val x2 = event.getX(1)
        val y2 = event.getY(1)
        return (y2 - y1) / (x2 - x1)
    }
}

interface OnRotateListener {
    fun onRotate(degrees: Float, focusX: Float, focusY: Float)
}