/*
 * Copyright 2020 Mupceet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.legado.app.ui.widget.recycler

import android.content.res.Resources
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import io.legado.app.BuildConfig
import io.legado.app.ui.widget.recycler.DragSelectTouchHelper.AdvanceCallback.Mode
import io.legado.app.utils.DebugLog

import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * @author mupceet
 *                        !autoChangeMode           +-------------------+     inactiveSelect()
 *           +------------------------------------> |                   | <--------------------+
 *           |                                      |      Normal       |                      |
 *           |        activeDragSelect(position)    |                   | activeSlideSelect()  |
 *           |      +------------------------------ |                   | ----------+          |
 *           |      v                               +-------------------+           v          |
 *  +-------------------+                              autoChangeMode     +-----------------------+
 *  | Drag From Disable | ----------------------------------------------> |                       |
 *  +-------------------+                                                 |                       |
 *  |                   |                                                 |                       |
 *  |                   | activeDragSelect(position) && allowDragInSlide  |        Slide          |
 *  |                   | <---------------------------------------------- |                       |
 *  |  Drag From Slide  |                                                 |                       |
 *  |                   |                                                 |                       |
 *  |                   | ----------------------------------------------> |                       |
 *  +-------------------+                                                 +-----------------------+
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class DragSelectTouchHelper(
    /**
     * Developer callback which controls the behavior of DragSelectTouchHelper.
     */
    private val mCallback: Callback,
) {

    companion object {
        private const val TAG = "DSTH"
        private const val MAX_HOTSPOT_RATIO = 0.5f
        private val DEFAULT_EDGE_TYPE = EdgeType.INSIDE_EXTEND
        private const val DEFAULT_HOTSPOT_RATIO = 0.2f
        private const val DEFAULT_HOTSPOT_OFFSET = 0
        private const val DEFAULT_MAX_SCROLL_VELOCITY = 10
        private const val SELECT_STATE_NORMAL = 0x00
        private const val SELECT_STATE_SLIDE = 0x01
        private const val SELECT_STATE_DRAG_FROM_NORMAL = 0x10
        private const val SELECT_STATE_DRAG_FROM_SLIDE = 0x11
    }

    private val mDisplayMetrics: DisplayMetrics = Resources.getSystem().displayMetrics

    /**
     * Start of the slide area.
     */
    private var mSlideAreaLeft = 0f

    /**
     * End of the slide area.
     */
    private var mSlideAreaRight = 0f

    /**
     * The hotspot height by the ratio of RecyclerView.
     */
    private var mHotspotHeightRatio = 0f

    /**
     * The hotspot height.
     */
    private var mHotspotHeight = 0f

    /**
     * The hotspot offset.
     */
    private var mHotspotOffset = 0f

    /**
     * Whether should continue scrolling when move outside top hotspot region.
     */
    private var mScrollAboveTopRegion = false

    /**
     * Whether should continue scrolling when move outside bottom hotspot region.
     */
    private var mScrollBelowBottomRegion = false

    /**
     * The maximum velocity of auto scrolling.
     */
    private var mMaximumVelocity = 0

    /**
     * Whether should auto enter slide mode after drag select finished.
     */
    private var mShouldAutoChangeState = false

    /**
     * Whether can drag selection in slide select mode.
     */
    private var mIsAllowDragInSlideState = false
    private var mRecyclerView: RecyclerView? = null

    /**
     * The coordinate of hotspot area.
     */
    private var mTopRegionFrom = -1f
    private var mTopRegionTo = -1f
    private var mBottomRegionFrom = -1f
    private var mBottomRegionTo = -1f
    private val mOnLayoutChangeListener =
        View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (oldLeft != left || oldRight != right || oldTop != top || oldBottom != bottom) {
                if (v === mRecyclerView) {
                    Logger.i(
                        "onLayoutChange:new: "
                                + left + " " + top + " " + right + " " + bottom
                    )
                    Logger.i(
                        "onLayoutChange:old: "
                                + oldLeft + " " + oldTop + " " + oldRight + " " + oldBottom
                    )
                    init(bottom - top)
                }
            }
        }

    /**
     * The current mode of selection.
     */
    private var mSelectState = SELECT_STATE_NORMAL

    /**
     * Whether is in top hotspot area.
     */
    private var mIsInTopHotspot = false

    /**
     * Whether is in bottom hotspot area.
     */
    private var mIsInBottomHotspot = false

    /**
     * Indicates automatically scroll.
     */
    private var mIsScrolling = false

    /**
     * The actual speed of the current moment.
     */
    private var mScrollDistance = 0

    /**
     * The reference coordinate for the action start, used to avoid reverse scrolling.
     */
    private var mDownY = Float.MIN_VALUE

    /**
     * The reference coordinates for the last action.
     */
    private var mLastX = Float.MIN_VALUE
    private var mLastY = Float.MIN_VALUE

    /**
     * The selected items position.
     */
    private var mStart = RecyclerView.NO_POSITION
    private var mEnd = RecyclerView.NO_POSITION
    private var mLastRealStart = RecyclerView.NO_POSITION
    private var mLastRealEnd = RecyclerView.NO_POSITION
    private var mSlideStateStartPosition = RecyclerView.NO_POSITION
    private var mHaveCalledSelectStart = false
    private val mScrollRunnable: Runnable by lazy {
        object : Runnable {
            override fun run() {
                if (mIsScrolling) {
                    scrollBy(mScrollDistance)
                    ViewCompat.postOnAnimation(mRecyclerView!!, this)
                }
            }
        }
    }
    private val mOnItemTouchListener: OnItemTouchListener by lazy {
        object : OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                Logger.d(
                    "onInterceptTouchEvent: x:" + e.x + ",y:" + e.y
                            + ", " + MotionEvent.actionToString(e.action)
                )
                val adapter = rv.adapter
                if (adapter == null || adapter.itemCount == 0) {
                    return false
                }
                var intercept = false
                val action = e.action
                when (action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_DOWN -> {
                        mDownY = e.y
                        // call the selection start's callback before moving
                        if (mSelectState == SELECT_STATE_SLIDE && isInSlideArea(e)) {
                            mSlideStateStartPosition = getItemPosition(rv, e)
                            if (mSlideStateStartPosition != RecyclerView.NO_POSITION) {
                                mCallback.onSelectStart(mSlideStateStartPosition)
                                mHaveCalledSelectStart = true
                            }
                            intercept = true
                        }
                    }
                    MotionEvent.ACTION_MOVE -> if (mSelectState == SELECT_STATE_DRAG_FROM_NORMAL
                        || mSelectState == SELECT_STATE_DRAG_FROM_SLIDE
                    ) {
                        Logger.i("onInterceptTouchEvent: drag mode move")
                        intercept = true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (mSelectState == SELECT_STATE_DRAG_FROM_NORMAL
                            || mSelectState == SELECT_STATE_DRAG_FROM_SLIDE
                        ) {
                            intercept = true
                        }
                        // finger is lifted before moving
                        if (mSlideStateStartPosition != RecyclerView.NO_POSITION) {
                            selectFinished(mSlideStateStartPosition)
                            mSlideStateStartPosition = RecyclerView.NO_POSITION
                        }
                        // selection has triggered
                        if (mStart != RecyclerView.NO_POSITION) {
                            selectFinished(mEnd)
                        }
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        if (mSlideStateStartPosition != RecyclerView.NO_POSITION) {
                            selectFinished(mSlideStateStartPosition)
                            mSlideStateStartPosition = RecyclerView.NO_POSITION
                        }
                        if (mStart != RecyclerView.NO_POSITION) {
                            selectFinished(mEnd)
                        }
                    }
                    else -> {
                    }
                }
                // Intercept only when the selection is triggered
                Logger.d("intercept result: $intercept")
                return intercept
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                if (!isActivated) {
                    return
                }
                Logger.d(
                    "onTouchEvent: x:" + e.x + ",y:" + e.y
                            + ", " + MotionEvent.actionToString(e.action)
                )
                val action = e.action
                when (action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_MOVE -> {
                        if (mSlideStateStartPosition != RecyclerView.NO_POSITION) {
                            selectFirstItem(mSlideStateStartPosition)
                            // selection is triggered
                            mSlideStateStartPosition = RecyclerView.NO_POSITION
                            Logger.i("onTouchEvent: after slide mode down")
                        }
                        processAutoScroll(e)
                        if (!mIsInTopHotspot && !mIsInBottomHotspot) {
                            updateSelectedRange(rv, e)
                        }
                    }
                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                        if (mSlideStateStartPosition != RecyclerView.NO_POSITION) {
                            selectFirstItem(mSlideStateStartPosition)
                            // selection is triggered
                            mSlideStateStartPosition = RecyclerView.NO_POSITION
                            Logger.i("onTouchEvent: after slide mode down")
                        }
                        if (!mIsInTopHotspot && !mIsInBottomHotspot) {
                            updateSelectedRange(rv, e)
                        }
                        selectFinished(mEnd)
                    }
                }
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                if (disallowIntercept) {
                    inactiveSelect()
                }
            }
        }
    }

    init {
        setHotspotRatio(DEFAULT_HOTSPOT_RATIO)
        setHotspotOffset(DEFAULT_HOTSPOT_OFFSET)
        setMaximumVelocity(DEFAULT_MAX_SCROLL_VELOCITY)
        setEdgeType(DEFAULT_EDGE_TYPE)
        setAutoEnterSlideState(false)
        setAllowDragInSlideState(false)
        setSlideArea(0, 0)
    }

    /**
     * Attaches the DragSelectTouchHelper to the provided RecyclerView. If TouchHelper is already
     * attached to a RecyclerView, it will first detach from the previous one. You can call this
     * method with `null` to detach it from the current RecyclerView.
     *
     * @param recyclerView The RecyclerView instance to which you want to add this helper or
     * `null` if you want to remove DragSelectTouchHelper from the
     * current RecyclerView.
     */
    fun attachToRecyclerView(recyclerView: RecyclerView?) {
        if (mRecyclerView === recyclerView) {
            return  // nothing to do
        }
        mRecyclerView?.removeOnItemTouchListener(mOnItemTouchListener)
        mRecyclerView = recyclerView
        mRecyclerView?.let {
            it.addOnItemTouchListener(mOnItemTouchListener)
            it.addOnLayoutChangeListener(mOnLayoutChangeListener)
        }
    }

    /**
     * Activate the slide selection mode.
     */
    fun activeSlideSelect() {
        activeSelectInternal(RecyclerView.NO_POSITION)
    }

    /**
     * Activate the selection mode with selected item position. Normally called on long press.
     *
     * @param position Indicates the position of selected item.
     */
    fun activeDragSelect(position: Int) {
        activeSelectInternal(position)
    }

    /**
     * Exit the selection mode.
     */
    fun inactiveSelect() {
        if (isActivated) {
            selectFinished(mEnd)
        } else {
            selectFinished(RecyclerView.NO_POSITION)
        }
        Logger.logSelectStateChange(mSelectState, SELECT_STATE_NORMAL)
        mSelectState = SELECT_STATE_NORMAL
    }

    /**
     * To determine whether it is in the selection mode.
     *
     * @return true if is in the selection mode.
     */
    val isActivated: Boolean
        get() = mSelectState != SELECT_STATE_NORMAL

    /**
     * Sets hotspot height by ratio of RecyclerView.
     *
     * @param ratio range (0, 0.5).
     * @return The select helper, which may used to chain setter calls.
     */
    fun setHotspotRatio(ratio: Float): DragSelectTouchHelper {
        mHotspotHeightRatio = ratio
        return this
    }

    /**
     * Sets hotspot height.
     *
     * @param hotspotHeight hotspot height which unit is dp.
     * @return The select helper, which may used to chain setter calls.
     */
    fun setHotspotHeight(hotspotHeight: Int): DragSelectTouchHelper {
        mHotspotHeight = dp2px(hotspotHeight.toFloat()).toFloat()
        return this
    }

    /**
     * Sets hotspot offset. It don't need to be set if no special requirement.
     *
     * @param hotspotOffset hotspot offset which unit is dp.
     * @return The select helper, which may used to chain setter calls.
     */
    fun setHotspotOffset(hotspotOffset: Int): DragSelectTouchHelper {
        mHotspotOffset = dp2px(hotspotOffset.toFloat()).toFloat()
        return this
    }

    /**
     * Sets the activation edge type, one of:
     *
     *  * [EdgeType.INSIDE] for edges that respond to touches inside
     * the bounds of the host view. If touch moves outside the bounds, scrolling
     * will stop.
     *  * [EdgeType.INSIDE_EXTEND] for inside edges that continued to
     * scroll when touch moves outside the bounds of the host view.
     *
     *
     * @param type The type of edge to use.
     * @return The select helper, which may used to chain setter calls.
     */
    fun setEdgeType(type: EdgeType?): DragSelectTouchHelper {
        when (type) {
            EdgeType.INSIDE -> {
                mScrollAboveTopRegion = false
                mScrollBelowBottomRegion = false
            }
            EdgeType.INSIDE_EXTEND -> {
                mScrollAboveTopRegion = true
                mScrollBelowBottomRegion = true
            }
            else -> {
                mScrollAboveTopRegion = true
                mScrollBelowBottomRegion = true
            }
        }
        return this
    }

    /**
     * Sets sliding area's start and end, has been considered RTL situation
     *
     * @param startDp The start of the sliding area
     * @param endDp   The end of the sliding area
     * @return The select helper, which may used to chain setter calls.
     */
    fun setSlideArea(startDp: Int, endDp: Int): DragSelectTouchHelper {
        if (!isRtl) {
            mSlideAreaLeft = dp2px(startDp.toFloat()).toFloat()
            mSlideAreaRight = dp2px(endDp.toFloat()).toFloat()
        } else {
            val displayWidth = mDisplayMetrics.widthPixels
            mSlideAreaLeft = displayWidth - dp2px(endDp.toFloat()).toFloat()
            mSlideAreaRight = displayWidth - dp2px(startDp.toFloat()).toFloat()
        }
        return this
    }

    /**
     * Sets the maximum velocity for scrolling
     *
     * @param velocity maximum velocity
     * @return The select helper, which may used to chain setter calls.
     */
    fun setMaximumVelocity(velocity: Int): DragSelectTouchHelper {
        mMaximumVelocity = (velocity * mDisplayMetrics.density + 0.5f).toInt()
        return this
    }

    /**
     * Sets whether should auto enter slide mode after drag select finished.
     * It's usefully for LinearLayout RecyclerView.
     *
     * @param autoEnterSlideState should auto enter slide mode
     * @return The select helper, which may used to chain setter calls.
     */
    fun setAutoEnterSlideState(autoEnterSlideState: Boolean): DragSelectTouchHelper {
        mShouldAutoChangeState = autoEnterSlideState
        return this
    }

    /**
     * Sets whether can drag selection in slide select mode.
     * It's usefully for LinearLayout RecyclerView.
     *
     * @param allowDragInSlideState allow drag selection in slide select mode
     * @return The select helper, which may used to chain setter calls.
     */
    fun setAllowDragInSlideState(allowDragInSlideState: Boolean): DragSelectTouchHelper {
        mIsAllowDragInSlideState = allowDragInSlideState
        return this
    }

    private fun init(rvHeight: Int) {
        if (mHotspotOffset >= rvHeight * MAX_HOTSPOT_RATIO) {
            mHotspotOffset = rvHeight * MAX_HOTSPOT_RATIO
        }
        // The height of hotspot area is not set, using (RV height x ratio)
        if (mHotspotHeight <= 0) {
            if (mHotspotHeightRatio <= 0 || mHotspotHeightRatio >= MAX_HOTSPOT_RATIO) {
                mHotspotHeightRatio = DEFAULT_HOTSPOT_RATIO
            }
            mHotspotHeight = rvHeight * mHotspotHeightRatio
        } else {
            if (mHotspotHeight >= rvHeight * MAX_HOTSPOT_RATIO) {
                mHotspotHeight = rvHeight * MAX_HOTSPOT_RATIO
            }
        }
        mTopRegionFrom = mHotspotOffset
        mTopRegionTo = mTopRegionFrom + mHotspotHeight
        mBottomRegionTo = rvHeight - mHotspotOffset
        mBottomRegionFrom = mBottomRegionTo - mHotspotHeight
        if (mTopRegionTo > mBottomRegionFrom) {
            mBottomRegionFrom = (rvHeight shr 1.toFloat().toInt()).toFloat()
            mTopRegionTo = mBottomRegionFrom
        }
        Logger.d(
            "Hotspot: [" + mTopRegionFrom + ", " + mTopRegionTo + "], ["
                    + mBottomRegionFrom + ", " + mBottomRegionTo + "]"
        )
    }

    private fun activeSelectInternal(position: Int) {
        // We should initialize the hotspot here, because its data may be delayed load
        mRecyclerView?.let {
            init(it.height)
        }
        if (position == RecyclerView.NO_POSITION) {
            Logger.logSelectStateChange(mSelectState, SELECT_STATE_SLIDE)
            mSelectState = SELECT_STATE_SLIDE
        } else {
            if (!mHaveCalledSelectStart) {
                mCallback.onSelectStart(position)
                mHaveCalledSelectStart = true
            }
            if (mSelectState == SELECT_STATE_SLIDE) {
                if (mIsAllowDragInSlideState && selectFirstItem(position)) {
                    Logger.logSelectStateChange(mSelectState, SELECT_STATE_DRAG_FROM_SLIDE)
                    mSelectState = SELECT_STATE_DRAG_FROM_SLIDE
                }
            } else if (mSelectState == SELECT_STATE_NORMAL) {
                if (selectFirstItem(position)) {
                    Logger.logSelectStateChange(mSelectState, SELECT_STATE_DRAG_FROM_NORMAL)
                    mSelectState = SELECT_STATE_DRAG_FROM_NORMAL
                }
            } else {
                Logger.e("activeSelect in unexpected state: $mSelectState")
            }
        }
    }

    private fun selectFirstItem(position: Int): Boolean {
        val selectFirstItemSucceed = mCallback.onSelectChange(position, true)
        // The drag select feature is only available if the first item is available for selection
        if (selectFirstItemSucceed) {
            mStart = position
            mEnd = position
            mLastRealStart = position
            mLastRealEnd = position
        }
        return selectFirstItemSucceed
    }

    private fun updateSelectedRange(rv: RecyclerView, e: MotionEvent) {
        updateSelectedRange(rv, e.x, e.y)
    }

    private fun updateSelectedRange(rv: RecyclerView, x: Float, y: Float) {
        val position = getItemPosition(rv, x, y)
        if (position != RecyclerView.NO_POSITION && mEnd != position) {
            mEnd = position
            notifySelectRangeChange()
        }
    }

    private fun notifySelectRangeChange() {
        if (mStart == RecyclerView.NO_POSITION || mEnd == RecyclerView.NO_POSITION) {
            return
        }
        val newStart: Int = min(mStart, mEnd)
        val newEnd: Int = max(mStart, mEnd)
        if (mLastRealStart == RecyclerView.NO_POSITION || mLastRealEnd == RecyclerView.NO_POSITION) {
            if (newEnd - newStart == 1) {
                notifySelectChange(newStart, newStart, true)
            } else {
                notifySelectChange(newStart, newEnd, true)
            }
        } else {
            if (newStart > mLastRealStart) {
                notifySelectChange(mLastRealStart, newStart - 1, false)
            } else if (newStart < mLastRealStart) {
                notifySelectChange(newStart, mLastRealStart - 1, true)
            }
            if (newEnd > mLastRealEnd) {
                notifySelectChange(mLastRealEnd + 1, newEnd, true)
            } else if (newEnd < mLastRealEnd) {
                notifySelectChange(newEnd + 1, mLastRealEnd, false)
            }
        }
        mLastRealStart = newStart
        mLastRealEnd = newEnd
    }

    private fun notifySelectChange(start: Int, end: Int, newState: Boolean) {
        for (i in start..end) {
            mCallback.onSelectChange(i, newState)
        }
    }

    private fun selectFinished(lastItem: Int) {
        if (lastItem != RecyclerView.NO_POSITION) {
            mCallback.onSelectEnd(lastItem)
        }
        mStart = RecyclerView.NO_POSITION
        mEnd = RecyclerView.NO_POSITION
        mLastRealStart = RecyclerView.NO_POSITION
        mLastRealEnd = RecyclerView.NO_POSITION
        mHaveCalledSelectStart = false
        mIsInTopHotspot = false
        mIsInBottomHotspot = false
        stopAutoScroll()
        when (mSelectState) {
            SELECT_STATE_DRAG_FROM_NORMAL -> mSelectState = if (mShouldAutoChangeState) {
                Logger.logSelectStateChange(
                    mSelectState,
                    SELECT_STATE_SLIDE
                )
                SELECT_STATE_SLIDE
            } else {
                Logger.logSelectStateChange(
                    mSelectState,
                    SELECT_STATE_NORMAL
                )
                SELECT_STATE_NORMAL
            }
            SELECT_STATE_DRAG_FROM_SLIDE -> {
                Logger.logSelectStateChange(mSelectState, SELECT_STATE_SLIDE)
                mSelectState = SELECT_STATE_SLIDE
            }
            else -> {
            }
        }
    }

    /**
     * Process motion event, according to the location to determine whether to scroll
     */
    private fun processAutoScroll(e: MotionEvent) {
        val y = e.y
        if (y in mTopRegionFrom..mTopRegionTo && y < mDownY) {
            mLastX = e.x
            mLastY = e.y
            val scrollDistanceFactor = (y - mTopRegionTo) / mHotspotHeight
            mScrollDistance = (mMaximumVelocity * scrollDistanceFactor).toInt()
            if (!mIsInTopHotspot) {
                mIsInTopHotspot = true
                startAutoScroll()
                mDownY = mTopRegionTo
            }
        } else if (mScrollAboveTopRegion && y < mTopRegionFrom && mIsInTopHotspot) {
            mLastX = e.x
            mLastY = mTopRegionFrom
            // Use the maximum speed
            mScrollDistance = mMaximumVelocity * -1
            startAutoScroll()
        } else if (y in mBottomRegionFrom..mBottomRegionTo && y > mDownY) {
            mLastX = e.x
            mLastY = e.y
            val scrollDistanceFactor = (y - mBottomRegionFrom) / mHotspotHeight
            mScrollDistance = (mMaximumVelocity * scrollDistanceFactor).toInt()
            if (!mIsInBottomHotspot) {
                mIsInBottomHotspot = true
                startAutoScroll()
                mDownY = mBottomRegionFrom
            }
        } else if (mScrollBelowBottomRegion && y > mBottomRegionTo && mIsInBottomHotspot) {
            mLastX = e.x
            mLastY = mBottomRegionTo
            // Use the maximum speed
            mScrollDistance = mMaximumVelocity
            startAutoScroll()
        } else {
            mIsInTopHotspot = false
            mIsInBottomHotspot = false
            mLastX = Float.MIN_VALUE
            mLastY = Float.MIN_VALUE
            stopAutoScroll()
        }
    }

    private fun startAutoScroll() {
        if (!mIsScrolling) {
            mIsScrolling = true
            mRecyclerView!!.removeCallbacks(mScrollRunnable)
            ViewCompat.postOnAnimation(mRecyclerView!!, mScrollRunnable)
        }
    }

    private fun stopAutoScroll() {
        if (mIsScrolling) {
            mIsScrolling = false
            mRecyclerView?.removeCallbacks(mScrollRunnable)
        }
    }

    private fun scrollBy(distance: Int) {
        val scrollDistance: Int =
            if (distance > 0) {
                min(distance, mMaximumVelocity)
            } else {
                max(distance, -mMaximumVelocity)
            }
        mRecyclerView!!.scrollBy(0, scrollDistance)
        if (mLastX != Float.MIN_VALUE && mLastY != Float.MIN_VALUE) {
            updateSelectedRange(mRecyclerView!!, mLastX, mLastY)
        }
    }

    private fun dp2px(dpVal: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpVal, mDisplayMetrics
        ).toInt()
    }

    private val isRtl: Boolean
        get() = (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                == View.LAYOUT_DIRECTION_RTL)

    private fun isInSlideArea(e: MotionEvent): Boolean {
        val x = e.x
        return x > mSlideAreaLeft && x < mSlideAreaRight
    }

    private fun getItemPosition(rv: RecyclerView, e: MotionEvent): Int {
        return getItemPosition(rv, e.x, e.y)
    }

    private fun getItemPosition(rv: RecyclerView, x: Float, y: Float): Int {
        val v = rv.findChildViewUnder(x, y)
        if (v == null) {
            val layoutManager = rv.layoutManager
            if (layoutManager is GridLayoutManager) {
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val lastItemPosition = layoutManager.getItemCount() - 1
                if (lastItemPosition == lastVisibleItemPosition) {
                    return lastItemPosition
                }
            }
            return RecyclerView.NO_POSITION
        }
        return rv.getChildAdapterPosition(v)
    }

    /**
     * Edge type that specifies an activation area starting at the view bounds and extending inward.
     */
    enum class EdgeType {
        /**
         * After activation begins, moving outside the view bounds will stop scrolling.
         */
        INSIDE,

        /**
         * After activation begins, moving outside the view bounds will continue scrolling.
         */
        INSIDE_EXTEND
    }

    /**
     * This class is the contract between DragSelectTouchHelper and your application. It lets you
     * update adapter when selection start/end and state changed.
     */
    abstract class Callback {
        /**
         * Called when changing item state.
         *
         * @param position   this item want to change the state to new state.
         * @param isSelected true if the position should be selected, false otherwise.
         * @return Whether to set the new state successfully.
         */
        abstract fun onSelectChange(position: Int, isSelected: Boolean): Boolean

        /**
         * Called when selection start.
         *
         * @param start the first selected item.
         */
        open fun onSelectStart(start: Int) {}

        /**
         * Called when selection end.
         *
         * @param end the last selected item.
         */
        open fun onSelectEnd(end: Int) {}
    }

    /**
     * An advance Callback which provide 4 useful selection modes [Mode].
     *
     *
     * Note: Since the state of item may be repeatedly set, in order to improve efficiency,
     * please process it in the Adapter
     */
    abstract class AdvanceCallback<T> : Callback {
        private var mMode: Mode? = null
        private var mOriginalSelection: MutableSet<T> = mutableSetOf()
        private var mFirstWasSelected = false

        /**
         * Creates a SimpleCallback with default [Mode.SelectAndReverse]# mode.
         *
         * @see Mode
         */
        constructor() {
            setMode(Mode.SelectAndReverse)
        }

        /**
         * Creates a SimpleCallback with select mode.
         *
         * @param mode the initial select mode
         * @see Mode
         */
        constructor(mode: Mode?) {
            setMode(mode)
        }

        /**
         * Sets the select mode.
         *
         * @param mode The type of select mode.
         * @see Mode
         */
        fun setMode(mode: Mode?) {
            mMode = mode
        }

        override fun onSelectStart(start: Int) {
            mOriginalSelection.clear()
            val selected = currentSelectedId()
            if (selected != null) {
                mOriginalSelection.addAll(selected)
            }
            mFirstWasSelected = mOriginalSelection.contains(getItemId(start))
        }

        override fun onSelectEnd(end: Int) {
            mOriginalSelection.clear()
        }

        override fun onSelectChange(position: Int, isSelected: Boolean): Boolean {
            return when (mMode) {
                Mode.SelectAndKeep -> {
                    updateSelectState(position, true)
                }
                Mode.SelectAndReverse -> {
                    updateSelectState(position, isSelected)
                }
                Mode.SelectAndUndo -> {
                    if (isSelected) {
                        updateSelectState(position, true)
                    } else {
                        updateSelectState(
                            position,
                            mOriginalSelection.contains(getItemId(position))
                        )
                    }
                }
                Mode.ToggleAndKeep -> {
                    updateSelectState(position, !mFirstWasSelected)
                }
                Mode.ToggleAndReverse -> {
                    if (isSelected) {
                        updateSelectState(position, !mFirstWasSelected)
                    } else {
                        updateSelectState(position, mFirstWasSelected)
                    }
                }
                Mode.ToggleAndUndo -> {
                    if (isSelected) {
                        updateSelectState(position, !mFirstWasSelected)
                    } else {
                        updateSelectState(
                            position,
                            mOriginalSelection.contains(getItemId(position))
                        )
                    }
                }
                else ->                     // SelectAndReverse Mode
                    updateSelectState(position, isSelected)
            }
        }

        /**
         * Get the currently selected items when selecting first item.
         *
         * @return the currently selected item's id set.
         */
        abstract fun currentSelectedId(): Set<T>?

        /**
         * Get the ID of the item.
         *
         * @param position item position to be judged.
         * @return item's identity.
         */
        abstract fun getItemId(position: Int): T

        /**
         * Update the selection status of the position.
         *
         * @param position   the position who's selection state changed.
         * @param isSelected true if the position should be selected, false otherwise.
         * @return Whether to set the state successfully.
         */
        abstract fun updateSelectState(position: Int, isSelected: Boolean): Boolean

        /**
         * Different existing selection modes
         */
        enum class Mode {
            /**
             * Selects the first item and applies the same state to each item you go by
             * and keep the state on move back
             */
            SelectAndKeep,

            /**
             * Selects the first item and applies the same state to each item you go by
             * and applies inverted state on move back
             */
            SelectAndReverse,

            /**
             * Selects the first item and applies the same state to each item you go by
             * and reverts to the original state on move back
             */
            SelectAndUndo,

            /**
             * Toggles the first item and applies the same state to each item you go by
             * and keep the state on move back
             */
            ToggleAndKeep,

            /**
             * Toggles the first item and applies the same state to each item you go by
             * and applies inverted state on move back
             */
            ToggleAndReverse,

            /**
             * Toggles the first item and applies the same state to each item you go by
             * and reverts to the original state on move back
             */
            ToggleAndUndo
        }
    }

    private object Logger {
        private val DEBUG = BuildConfig.DEBUG
        fun d(msg: String) {
            DebugLog.d(javaClass.name, msg)
        }

        fun e(msg: String) {
            DebugLog.e(javaClass.name, msg)
        }

        fun i(msg: String) {
            DebugLog.i(javaClass.name, msg)
        }

        fun logSelectStateChange(before: Int, after: Int) {
            i("Select state changed: " + stateName(before) + " --> " + stateName(after))
        }

        private fun stateName(state: Int): String {
            return when (state) {
                SELECT_STATE_NORMAL -> "NormalState"
                SELECT_STATE_SLIDE -> "SlideState"
                SELECT_STATE_DRAG_FROM_NORMAL -> "DragFromNormal"
                SELECT_STATE_DRAG_FROM_SLIDE -> "DragFromSlide"
                else -> "Unknown"
            }
        }
    }


}