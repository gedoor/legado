package io.legado.app.ui.widget.page

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.animation.Interpolator
import android.widget.OverScroller
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import kotlin.math.abs


class ContentTextView : AppCompatTextView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    private val INVALID_POINTER = -1
    private val SCROLL_STATE_IDLE = 0
    private val SCROLL_STATE_DRAGGING = 1
    val SCROLL_STATE_SETTLING = 2

    private var velocityTracker: VelocityTracker? = null
    private var mScrollState = SCROLL_STATE_IDLE
    private var mScrollPointerId = INVALID_POINTER
    private var mLastTouchY: Int = 0
    private var mTouchSlop: Int = 0
    private var mMinFlingVelocity: Int = 0
    private var mMaxFlingVelocity: Int = 0
    private val mViewFling = ViewFling()

    //f(x) = (x-1)^5 + 1
    private val sQuinticInterpolator = Interpolator {
        var t = it
        t -= 1.0f
        t * t * t * t * t + 1.0f
    }

    init {
        val vc = ViewConfiguration.get(context)
        mTouchSlop = vc.scaledTouchSlop
        mMinFlingVelocity = vc.scaledMinimumFlingVelocity
        mMaxFlingVelocity = vc.scaledMaximumFlingVelocity
    }

    /**
     * 获取当前页总字数
     */
    fun getCharNum(lineNum: Int = getLineNum()): Int {
        return layout?.getLineEnd(lineNum) ?: 0
    }

    /**
     * 获取当前页总行数
     */
    fun getLineNum(): Int {
        val topOfLastLine = height - paddingTop - paddingBottom - lineHeight
        return layout?.getLineForVertical(topOfLastLine) ?: 0
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            val action = event.action
            val actionIndex = event.actionIndex
            val vtEvent = MotionEvent.obtain(event)

            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain()
            }
            velocityTracker?.addMovement(it)
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    setScrollState(SCROLL_STATE_IDLE)
                    mScrollPointerId = event.getPointerId(0)
                    mLastTouchY = (event.y + 0.5f).toInt()
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    mScrollPointerId = event.getPointerId(actionIndex)
                    mLastTouchY = (event.getY(actionIndex) + 0.5f).toInt()
                }
                MotionEvent.ACTION_MOVE -> {
                    val index = event.findPointerIndex(mScrollPointerId)
                    if (index < 0) {
                        return false
                    }

                    val y = (event.getY(index) + 0.5f).toInt()
                    var dy = mLastTouchY - y

                    if (mScrollState != SCROLL_STATE_DRAGGING) {
                        var startScroll = false

                        if (abs(dy) > mTouchSlop) {
                            if (dy > 0) {
                                dy -= mTouchSlop
                            } else {
                                dy += mTouchSlop
                            }
                            startScroll = true
                        }
                        if (startScroll) {
                            setScrollState(SCROLL_STATE_DRAGGING)
                        }
                    }

                    if (mScrollState == SCROLL_STATE_DRAGGING) {
                        mLastTouchY = y
                    }
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    if (event.getPointerId(actionIndex) == mScrollPointerId) {
                        // Pick a new pointer to pick up the slack.
                        val newIndex = if (actionIndex == 0) 1 else 0
                        mScrollPointerId = event.getPointerId(newIndex)
                        mLastTouchY = (event.getY(newIndex) + 0.5f).toInt()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    velocityTracker?.computeCurrentVelocity(1000, mMaxFlingVelocity.toFloat())
                    val yVelocity = velocityTracker?.getYVelocity(mScrollPointerId) ?: 0f
                    if (abs(yVelocity) > mMinFlingVelocity) {
                        mViewFling.fling(-yVelocity.toInt())
                    } else {
                        setScrollState(SCROLL_STATE_IDLE)
                    }
                    resetTouch()
                }
                MotionEvent.ACTION_CANCEL -> {
                    resetTouch()
                }
            }
            vtEvent.recycle()
        }
        return super.onTouchEvent(event)
    }

    private fun resetTouch() {
        velocityTracker?.clear()
    }

    private fun setScrollState(state: Int) {
        if (state == mScrollState) {
            return
        }
        mScrollState = state
        if (state != SCROLL_STATE_SETTLING) {
            mViewFling.stop()
        }
    }

    private inner class ViewFling : Runnable {

        private var mLastFlingY = 0
        private val mScroller: OverScroller = OverScroller(context, sQuinticInterpolator)
        private var mEatRunOnAnimationRequest = false
        private var mReSchedulePostAnimationCallback = false

        override fun run() {
            disableRunOnAnimationRequests()
            val scroller = mScroller
            if (scroller.computeScrollOffset()) {
                val y = scroller.currY
                val dy = y - mLastFlingY
                mLastFlingY = y
                this@ContentTextView.scrollBy(0, dy)
                postOnAnimation()
            }
            enableRunOnAnimationRequests()
        }

        fun fling(velocityY: Int) {
            mLastFlingY = 0
            setScrollState(SCROLL_STATE_SETTLING)
            mScroller.fling(
                0,
                0,
                0,
                velocityY,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE
            )
            postOnAnimation()
        }

        fun stop() {
            removeCallbacks(this)
            mScroller.abortAnimation()
        }

        private fun disableRunOnAnimationRequests() {
            mReSchedulePostAnimationCallback = false
            mEatRunOnAnimationRequest = true
        }

        private fun enableRunOnAnimationRequests() {
            mEatRunOnAnimationRequest = false
            if (mReSchedulePostAnimationCallback) {
                postOnAnimation()
            }
        }

        internal fun postOnAnimation() {
            if (mEatRunOnAnimationRequest) {
                mReSchedulePostAnimationCallback = true
            } else {
                removeCallbacks(this)
                ViewCompat.postOnAnimation(this@ContentTextView, this)
            }
        }
    }
}
