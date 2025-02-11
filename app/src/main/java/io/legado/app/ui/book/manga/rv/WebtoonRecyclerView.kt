package io.legado.app.ui.book.manga.rv

import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.utils.findCenterViewPosition
import kotlin.math.abs

class WebtoonRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : RecyclerView(context, attrs, defStyle) {

    private var atLastPosition = false
    private var atFirstPosition = false
    private var firstVisibleItemPosition = 0
    private var lastVisibleItemPosition = 0

    private var mLastCenterViewPosition = 0

    private var mPreScrollListener: IComicPreScroll? = null
    private var mNestedPreScrollListener: IComicPreScroll? = null
    private var mToucheMiddle: (() -> Unit)? = null
    private val mcRect = RectF()
    private var isMove = false

    //起始点
    private var startX: Float = 0f
    private var startY: Float = 0f
    private val slopSquare by lazy { ViewConfiguration.get(context).scaledTouchSlop }

    fun onToucheMiddle(init: () -> Unit) = apply { this.mToucheMiddle = init }
    override fun onScrolled(dx: Int, dy: Int) {
        super.onScrolled(dx, dy)
        val layoutManager = layoutManager
        lastVisibleItemPosition =
            (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        val layoutManager = layoutManager
        val visibleItemCount = layoutManager?.childCount ?: 0
        val totalItemCount = layoutManager?.itemCount ?: 0
        atLastPosition = visibleItemCount > 0 && lastVisibleItemPosition == totalItemCount - 1
        atFirstPosition = firstVisibleItemPosition == 0
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int,
    ): Boolean {
        val position = findCenterViewPosition()
        if (position != NO_POSITION && position != mLastCenterViewPosition) {
            mLastCenterViewPosition = position
            mPreScrollListener?.onPreScrollListener(this, dx, dy, position)
        }
        mNestedPreScrollListener?.onPreScrollListener(this, dx, dy, position)
        return super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }

    fun setPreScrollListener(iComicPreScroll: IComicPreScroll) {
        mPreScrollListener = iComicPreScroll
    }

    fun setNestedPreScrollListener(iComicPreScroll: IComicPreScroll) {
        mNestedPreScrollListener = iComicPreScroll
    }

    fun interface IComicPreScroll {
        fun onPreScrollListener(recyclerView: RecyclerView, dx: Int, dy: Int, position: Int)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mcRect.set(width * 0.33f, height * 0.33f, width * 0.66f, height * 0.66f)
    }

    override fun dispatchTouchEvent(e: MotionEvent?): Boolean {
        when {
            e?.action == MotionEvent.ACTION_DOWN -> {
                startY = e.y
                startX = e.x
                isMove = false
            }

            e?.action == MotionEvent.ACTION_MOVE -> {
                val absX = abs(startX - e.x)
                val absY = abs(startY - e.y)
                if (!isMove) {
                    isMove = absX > slopSquare || absY > slopSquare
                }
            }

            e?.action == MotionEvent.ACTION_UP -> {
                if (mcRect.contains(startX, startY) && !isMove) {
                    mToucheMiddle?.invoke()
                }
            }
        }
        return super.dispatchTouchEvent(e)
    }
}
