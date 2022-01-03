package io.legado.app.ui.widget.recycler.scroller

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.widget.*
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import io.legado.app.R
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.getCompatColor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


@Suppress("SameParameterValue")
class FastScroller : LinearLayout {
    @ColorInt
    private var mBubbleColor: Int = 0

    @ColorInt
    private var mHandleColor: Int = 0
    private var mBubbleHeight: Int = 0
    private var mHandleHeight: Int = 0
    private var mViewHeight: Int = 0
    private var mFadeScrollbar: Boolean = false
    private var mShowBubble: Boolean = false
    private var mSectionIndexer: SectionIndexer? = null
    private var mScrollbarAnimator: ViewPropertyAnimator? = null
    private var mBubbleAnimator: ViewPropertyAnimator? = null
    private var mRecyclerView: RecyclerView? = null
    private lateinit var mBubbleView: TextView
    private lateinit var mHandleView: ImageView
    private lateinit var mTrackView: ImageView
    private lateinit var mScrollbar: View
    private var mBubbleImage: Drawable? = null
    private var mHandleImage: Drawable? = null
    private var mTrackImage: Drawable? = null
    private var mFastScrollStateChangeListener: FastScrollStateChangeListener? = null
    private val mScrollbarHider = Runnable { this.hideScrollbar() }

    private val mScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (!mHandleView.isSelected && isEnabled) {
                setViewPositions(getScrollProportion(recyclerView))
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (isEnabled) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        handler.removeCallbacks(mScrollbarHider)
                        cancelAnimation(mScrollbarAnimator)
                        if (!isViewVisible(mScrollbar)) {
                            showScrollbar()
                        }
                    }
                    RecyclerView.SCROLL_STATE_IDLE -> if (mFadeScrollbar && !mHandleView.isSelected) {
                        handler.postDelayed(mScrollbarHider, sScrollbarHideDelay.toLong())
                    }
                }
            }
        }
    }

    constructor(context: Context) : super(context) {
        layout(context, null)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
    }

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        layout(context, attrs)
        layoutParams = generateLayoutParams(attrs)
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams) {
        params.width = LayoutParams.WRAP_CONTENT
        super.setLayoutParams(params)
    }

    fun setLayoutParams(viewGroup: ViewGroup) {
        @IdRes val recyclerViewId = mRecyclerView?.id ?: View.NO_ID
        val marginTop = resources.getDimensionPixelSize(R.dimen.fastscroll_scrollbar_margin_top)
        val marginBottom =
            resources.getDimensionPixelSize(R.dimen.fastscroll_scrollbar_margin_bottom)
        require(recyclerViewId != View.NO_ID) { "RecyclerView must have a view ID" }
        when (viewGroup) {
            is ConstraintLayout -> {
                val constraintSet = ConstraintSet()
                @IdRes val layoutId = id
                constraintSet.clone(viewGroup)
                constraintSet.connect(
                    layoutId,
                    ConstraintSet.TOP,
                    recyclerViewId,
                    ConstraintSet.TOP
                )
                constraintSet.connect(
                    layoutId,
                    ConstraintSet.BOTTOM,
                    recyclerViewId,
                    ConstraintSet.BOTTOM
                )
                constraintSet.connect(
                    layoutId,
                    ConstraintSet.END,
                    recyclerViewId,
                    ConstraintSet.END
                )
                constraintSet.applyTo(viewGroup)
                val layoutParams = layoutParams as ConstraintLayout.LayoutParams
                layoutParams.setMargins(0, marginTop, 0, marginBottom)
                setLayoutParams(layoutParams)
            }
            is CoordinatorLayout -> {
                val layoutParams = layoutParams as CoordinatorLayout.LayoutParams
                layoutParams.anchorId = recyclerViewId
                layoutParams.anchorGravity = GravityCompat.END
                layoutParams.setMargins(0, marginTop, 0, marginBottom)
                setLayoutParams(layoutParams)
            }
            is FrameLayout -> {
                val layoutParams = layoutParams as FrameLayout.LayoutParams
                layoutParams.gravity = GravityCompat.END
                layoutParams.setMargins(0, marginTop, 0, marginBottom)
                setLayoutParams(layoutParams)
            }
            is RelativeLayout -> {
                val layoutParams = layoutParams as RelativeLayout.LayoutParams
                val endRule = RelativeLayout.ALIGN_END
                layoutParams.addRule(RelativeLayout.ALIGN_TOP, recyclerViewId)
                layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, recyclerViewId)
                layoutParams.addRule(endRule, recyclerViewId)
                layoutParams.setMargins(0, marginTop, 0, marginBottom)
                setLayoutParams(layoutParams)
            }
            else -> throw IllegalArgumentException("Parent ViewGroup must be a ConstraintLayout, CoordinatorLayout, FrameLayout, or RelativeLayout")
        }
        updateViewHeights()
    }

    fun setSectionIndexer(sectionIndexer: SectionIndexer?) {
        mSectionIndexer = sectionIndexer
    }

    fun attachRecyclerView(recyclerView: RecyclerView) {
        mRecyclerView = recyclerView
        mRecyclerView!!.addOnScrollListener(mScrollListener)
        post {
            // set initial positions for bubble and handle
            setViewPositions(getScrollProportion(mRecyclerView))
        }
    }

    fun detachRecyclerView() {
        if (mRecyclerView != null) {
            mRecyclerView!!.removeOnScrollListener(mScrollListener)
            mRecyclerView = null
        }
    }

    /**
     * Hide the scrollbar when not scrolling.
     *
     * @param fadeScrollbar True to hide the scrollbar, false to show
     */
    fun setFadeScrollbar(fadeScrollbar: Boolean) {
        mFadeScrollbar = fadeScrollbar
        mScrollbar.visibility = if (fadeScrollbar) View.INVISIBLE else View.VISIBLE
    }

    /**
     * Show the section bubble while scrolling.
     *
     * @param visible True to show the bubble, false to hide
     */
    fun setBubbleVisible(visible: Boolean) {
        mShowBubble = visible
    }

    /**
     * Display a scroll track while scrolling.
     *
     * @param visible True to show scroll track, false to hide
     */
    fun setTrackVisible(visible: Boolean) {
        mTrackView.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    /**
     * Set the color of the scroll track.
     *
     * @param color The color for the scroll track
     */
    fun setTrackColor(@ColorInt color: Int) {
        if (mTrackImage == null) {
            val drawable = ContextCompat.getDrawable(context, R.drawable.fastscroll_track)
            if (drawable != null) {
                mTrackImage = DrawableCompat.wrap(drawable)
            }
        }
        DrawableCompat.setTint(mTrackImage!!, color)
        mTrackView.setImageDrawable(mTrackImage)
    }

    /**
     * Set the color for the scroll handle.
     *
     * @param color The color for the scroll handle
     */
    fun setHandleColor(@ColorInt color: Int) {
        mHandleColor = color
        if (mHandleImage == null) {
            val drawable = ContextCompat.getDrawable(context, R.drawable.fastscroll_handle)
            if (drawable != null) {
                mHandleImage = DrawableCompat.wrap(drawable)
            }
        }
        DrawableCompat.setTint(mHandleImage!!, mHandleColor)
        mHandleView.setImageDrawable(mHandleImage)
    }

    /**
     * Set the background color of the index bubble.
     *
     * @param color The background color for the index bubble
     */
    fun setBubbleColor(@ColorInt color: Int) {
        mBubbleColor = color
        if (mBubbleImage == null) {
            val drawable = ContextCompat.getDrawable(context, R.drawable.fastscroll_bubble)
            if (drawable != null) {
                mBubbleImage = DrawableCompat.wrap(drawable)
            }
        }
        DrawableCompat.setTint(mBubbleImage!!, mBubbleColor)
        mBubbleView.background = mBubbleImage
    }

    /**
     * Set the text color of the index bubble.
     *
     * @param color The text color for the index bubble
     */
    fun setBubbleTextColor(@ColorInt color: Int) {
        mBubbleView.setTextColor(color)
    }

    /**
     * Set the fast scroll state change listener.
     *
     * @param fastScrollStateChangeListener The interface that will listen to fastscroll state change events
     */
    fun setFastScrollStateChangeListener(fastScrollStateChangeListener: FastScrollStateChangeListener) {
        mFastScrollStateChangeListener = fastScrollStateChangeListener
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        visibility = if (enabled) View.VISIBLE else View.INVISIBLE
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (event.x < mHandleView.x - ViewCompat.getPaddingStart(mHandleView)) {
                    return false
                }
                if (!mScrollbar.isVisible) {
                    return false
                }
                requestDisallowInterceptTouchEvent(true)
                setHandleSelected(true)
                handler.removeCallbacks(mScrollbarHider)
                cancelAnimation(mScrollbarAnimator)
                cancelAnimation(mBubbleAnimator)
                if (mShowBubble && mSectionIndexer != null) {
                    showBubble()
                }
                if (mFastScrollStateChangeListener != null) {
                    mFastScrollStateChangeListener!!.onFastScrollStart(this)
                }
                val y = event.y
                setViewPositions(y)
                setRecyclerViewPosition(y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val y = event.y
                setViewPositions(y)
                setRecyclerViewPosition(y)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                requestDisallowInterceptTouchEvent(false)
                setHandleSelected(false)
                if (mFadeScrollbar) {
                    handler.postDelayed(mScrollbarHider, sScrollbarHideDelay.toLong())
                }
                hideBubble()
                if (mFastScrollStateChangeListener != null) {
                    mFastScrollStateChangeListener!!.onFastScrollStop(this)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mViewHeight = h
    }

    private fun setRecyclerViewPosition(y: Float) {
        mRecyclerView?.adapter?.let { adapter ->
            val itemCount = adapter.itemCount
            val proportion: Float = when {
                mHandleView.y == 0f -> 0f
                mHandleView.y + mHandleHeight >= mViewHeight - sTrackSnapRange -> 1f
                else -> y / mViewHeight.toFloat()
            }
            var scrolledItemCount = (proportion * itemCount).roundToInt()
            if (isLayoutReversed(mRecyclerView?.layoutManager)) {
                scrolledItemCount = itemCount - scrolledItemCount
            }
            val targetPos = getValueInRange(0, itemCount - 1, scrolledItemCount)
            mRecyclerView?.layoutManager?.scrollToPosition(targetPos)
            mSectionIndexer?.let { sectionIndexer ->
                if (mShowBubble) {
                    mBubbleView.text = sectionIndexer.getSectionText(targetPos)
                }
            }
        }
    }

    private fun getScrollProportion(recyclerView: RecyclerView?): Float {
        recyclerView ?: return 0f
        val verticalScrollOffset = recyclerView.computeVerticalScrollOffset()
        val verticalScrollRange = recyclerView.computeVerticalScrollRange()
        val rangeDiff = (verticalScrollRange - mViewHeight).toFloat()
        val proportion = verticalScrollOffset.toFloat() / if (rangeDiff > 0) rangeDiff else 1f
        return mViewHeight * proportion
    }

    private fun getValueInRange(min: Int, max: Int, value: Int): Int {
        val minimum = max(min, value)
        return min(minimum, max)
    }

    private fun setViewPositions(y: Float) {
        mBubbleHeight = mBubbleView.height
        mHandleHeight = mHandleView.height
        val bubbleY = getValueInRange(
            0,
            mViewHeight - mBubbleHeight - mHandleHeight / 2,
            (y - mBubbleHeight).toInt()
        )
        val handleY =
            getValueInRange(0, mViewHeight - mHandleHeight, (y - mHandleHeight / 2).toInt())
        if (mShowBubble) {
            mBubbleView.y = bubbleY.toFloat()
        }
        mHandleView.y = handleY.toFloat()
    }

    private fun updateViewHeights() {
        val measureSpec =
            MeasureSpec.makeMeasureSpec(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        mBubbleView.measure(measureSpec, measureSpec)
        mBubbleHeight = mBubbleView.measuredHeight
        mHandleView.measure(measureSpec, measureSpec)
        mHandleHeight = mHandleView.measuredHeight
    }

    private fun isLayoutReversed(layoutManager: RecyclerView.LayoutManager?): Boolean {
        if (layoutManager is LinearLayoutManager) {
            return layoutManager.reverseLayout
        } else if (layoutManager is StaggeredGridLayoutManager) {
            return layoutManager.reverseLayout
        }
        return false
    }

    private fun isViewVisible(view: View?): Boolean {
        return view != null && view.visibility == View.VISIBLE
    }

    private fun cancelAnimation(animator: ViewPropertyAnimator?) {
        animator?.cancel()
    }

    private fun showBubble() {
        if (!isViewVisible(mBubbleView)) {
            mBubbleView.visibility = View.VISIBLE
            mBubbleAnimator = mBubbleView.animate().alpha(1f)
                .setDuration(sBubbleAnimDuration.toLong())
                .setListener(object : AnimatorListenerAdapter() {

                    // adapter required for new alpha value to stick
                })
        }
    }

    private fun hideBubble() {
        if (isViewVisible(mBubbleView)) {
            mBubbleAnimator = mBubbleView.animate().alpha(0f)
                .setDuration(sBubbleAnimDuration.toLong())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        mBubbleView.visibility = View.INVISIBLE
                        mBubbleAnimator = null
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        super.onAnimationCancel(animation)
                        mBubbleView.visibility = View.INVISIBLE
                        mBubbleAnimator = null
                    }
                })
        }
    }

    private fun showScrollbar() {
        mRecyclerView?.let { mRecyclerView ->
            if (mRecyclerView.computeVerticalScrollRange() - mViewHeight > 0) {
                val transX =
                    resources.getDimensionPixelSize(R.dimen.fastscroll_scrollbar_padding_end)
                        .toFloat()
                mScrollbar.translationX = transX
                mScrollbar.visibility = View.VISIBLE
                mScrollbarAnimator = mScrollbar.animate().translationX(0f).alpha(1f)
                    .setDuration(sScrollbarAnimDuration.toLong())
                    .setListener(object : AnimatorListenerAdapter() {

                        // adapter required for new alpha value to stick
                    })
            }
        }
    }

    private fun hideScrollbar() {
        val transX =
            resources.getDimensionPixelSize(R.dimen.fastscroll_scrollbar_padding_end).toFloat()
        mScrollbarAnimator = mScrollbar.animate().translationX(transX).alpha(0f)
            .setDuration(sScrollbarAnimDuration.toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mScrollbar.visibility = View.INVISIBLE
                    mScrollbarAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    mScrollbar.visibility = View.INVISIBLE
                    mScrollbarAnimator = null
                }
            })
    }

    private fun setHandleSelected(selected: Boolean) {
        mHandleView.isSelected = selected
        DrawableCompat.setTint(mHandleImage!!, if (selected) mBubbleColor else mHandleColor)
    }

    private fun layout(context: Context, attrs: AttributeSet?) {
        View.inflate(context, R.layout.view_fastscroller, this)
        clipChildren = false
        orientation = HORIZONTAL
        mBubbleView = findViewById(R.id.fastscroll_bubble)
        mHandleView = findViewById(R.id.fastscroll_handle)
        mTrackView = findViewById(R.id.fastscroll_track)
        mScrollbar = findViewById(R.id.fastscroll_scrollbar)
        @ColorInt var bubbleColor = ColorUtils.adjustAlpha(context.accentColor, 0.8f)
        @ColorInt var handleColor = context.accentColor
        @ColorInt var trackColor = context.getCompatColor(R.color.transparent30)
        @ColorInt var textColor =
            if (ColorUtils.isColorLight(bubbleColor)) Color.BLACK else Color.WHITE
        var fadeScrollbar = true
        var showBubble = false
        var showTrack = true
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.FastScroller, 0, 0)
            try {
                bubbleColor = typedArray.getColor(R.styleable.FastScroller_bubbleColor, bubbleColor)
                handleColor = typedArray.getColor(R.styleable.FastScroller_handleColor, handleColor)
                trackColor = typedArray.getColor(R.styleable.FastScroller_trackColor, trackColor)
                textColor = typedArray.getColor(R.styleable.FastScroller_bubbleTextColor, textColor)
                fadeScrollbar =
                    typedArray.getBoolean(R.styleable.FastScroller_fadeScrollbar, fadeScrollbar)
                showBubble = typedArray.getBoolean(R.styleable.FastScroller_showBubble, showBubble)
                showTrack = typedArray.getBoolean(R.styleable.FastScroller_showTrack, showTrack)
            } finally {
                typedArray.recycle()
            }
        }
        setTrackColor(trackColor)
        setHandleColor(handleColor)
        setBubbleColor(bubbleColor)
        setBubbleTextColor(textColor)
        setFadeScrollbar(fadeScrollbar)
        setBubbleVisible(showBubble)
        setTrackVisible(showTrack)
    }

    interface SectionIndexer {
        fun getSectionText(position: Int): String
    }

    companion object {
        private const val sBubbleAnimDuration = 100
        private const val sScrollbarAnimDuration = 300
        private const val sScrollbarHideDelay = 1000
        private const val sTrackSnapRange = 5
    }

}
