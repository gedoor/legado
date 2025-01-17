package io.legado.app.ui.book.manga.rv

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout

/**
 * Frame layout which contains a [WebtoonRecyclerView]. It's needed to handle touch events,
 * because the recyclerview is scaled and its touch events are translated, which breaks the
 * detectors.
 *
 * TODO consider integrating this class into [WebtoonViewer].
 */
class WebtoonFrame : FrameLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    /**
     * Scale detector, either with pinch or quick scale.
     */
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())

    /**
     * Fling detector.
     */
    private val flingDetector = GestureDetector(context, FlingListener())

    var doubleTapZoom = false
        set(value) {
            field = value
            recycler?.doubleTapZoom = value
            scaleDetector.isQuickScaleEnabled = value
        }

    /**
     * Recycler view added in this frame.
     */
    private val recycler: WebtoonRecyclerView?
        get() = getChildAt(0) as? WebtoonRecyclerView

    /**
     * Dispatches a touch event to the detectors.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(ev)
        flingDetector.onTouchEvent(ev)

        // Get the bounding box of the recyclerview and translate any motion events to be within it.
        // Used to allow scrolling outside the recyclerview.
        val recyclerRect = Rect()
        recycler?.getHitRect(recyclerRect) ?: return super.dispatchTouchEvent(ev)
        // Shrink the box to account for any rounding issues.
        recyclerRect.inset(1, 1)

        if (recyclerRect.right < recyclerRect.left || recyclerRect.bottom < recyclerRect.top) {
            return super.dispatchTouchEvent(ev)
        }

        ev.setLocation(
            ev.x.coerceIn(recyclerRect.left.toFloat(), recyclerRect.right.toFloat()),
            ev.y.coerceIn(recyclerRect.top.toFloat(), recyclerRect.bottom.toFloat()),
        )
        return super.dispatchTouchEvent(ev)
    }

    /**
     * Scale listener used to delegate events to the recycler view.
     */
    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            recycler?.onScaleBegin()
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            recycler?.onScale(detector.scaleFactor)
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            recycler?.onScaleEnd()
        }
    }

    /**
     * Fling listener used to delegate events to the recycler view.
     */
    inner class FlingListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float,
        ): Boolean {
            return recycler?.zoomFling(velocityX.toInt(), velocityY.toInt()) ?: false
        }
    }
}
