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

package io.legado.app.ui.widget.recycler;

import android.content.res.Resources;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import io.legado.app.BuildConfig;

/**
 * @author mupceet
 */
public class DragSelectTouchHelper {
    private static final String TAG = "DSTH";
    private static final float MAX_HOTSPOT_RATIO = 0.5f;
    private static final EdgeType DEFAULT_EDGE_TYPE = EdgeType.INSIDE_EXTEND;
    private static final float DEFAULT_HOTSPOT_RATIO = 0.2f;
    private static final int DEFAULT_HOTSPOT_OFFSET = 0;
    private static final int DEFAULT_MAX_SCROLL_VELOCITY = 10;
    private static final int SELECT_STATE_NORMAL = 0x00;

    /*
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

    private static final int SELECT_STATE_SLIDE = 0x01;
    private static final int SELECT_STATE_DRAG_FROM_NORMAL = 0x10;
    private static final int SELECT_STATE_DRAG_FROM_SLIDE = 0x11;
    private final DisplayMetrics mDisplayMetrics;
    /**
     * Start of the slide area.
     */
    private float mSlideAreaLeft;
    /**
     * End of the slide area.
     */
    private float mSlideAreaRight;
    /**
     * The hotspot height by the ratio of RecyclerView.
     */
    private float mHotspotHeightRatio;
    /**
     * The hotspot height.
     */
    private float mHotspotHeight = 0f;
    /**
     * The hotspot offset.
     */
    private float mHotspotOffset;
    /**
     * Whether should continue scrolling when move outside top hotspot region.
     */
    private boolean mScrollAboveTopRegion;
    /**
     * Whether should continue scrolling when move outside bottom hotspot region.
     */
    private boolean mScrollBelowBottomRegion;
    /**
     * The maximum velocity of auto scrolling.
     */
    private int mMaximumVelocity;
    /**
     * Whether should auto enter slide mode after drag select finished.
     */
    private boolean mShouldAutoChangeState;
    /**
     * Whether can drag selection in slide select mode.
     */
    private boolean mIsAllowDragInSlideState;
    private RecyclerView mRecyclerView = null;
    /**
     * The coordinate of hotspot area.
     */
    private float mTopRegionFrom = -1f;
    private float mTopRegionTo = -1f;
    private float mBottomRegionFrom = -1f;
    private float mBottomRegionTo = -1f;
    private final View.OnLayoutChangeListener mOnLayoutChangeListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                   int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if (oldLeft != left || oldRight != right || oldTop != top || oldBottom != bottom) {
                if (v == mRecyclerView) {
                    Logger.i("onLayoutChange:new: "
                            + left + " " + top + " " + right + " " + bottom);
                    Logger.i("onLayoutChange:old: "
                            + oldLeft + " " + oldTop + " " + oldRight + " " + oldBottom);
                    init(bottom - top);
                }
            }
        }
    };
    /**
     * The current mode of selection.
     */
    private int mSelectState = SELECT_STATE_NORMAL;
    /**
     * Whether is in top hotspot area.
     */
    private boolean mIsInTopHotspot = false;
    /**
     * Whether is in bottom hotspot area.
     */
    private boolean mIsInBottomHotspot = false;
    /**
     * Indicates automatically scroll.
     */
    private boolean mIsScrolling;
    /**
     * The actual speed of the current moment.
     */
    private int mScrollDistance = 0;
    /**
     * The reference coordinate for the action start, used to avoid reverse scrolling.
     */
    private float mDownY = Float.MIN_VALUE;
    /**
     * The reference coordinates for the last action.
     */
    private float mLastX = Float.MIN_VALUE;
    private float mLastY = Float.MIN_VALUE;
    /**
     * The selected items position.
     */
    private int mStart = RecyclerView.NO_POSITION;
    private int mEnd = RecyclerView.NO_POSITION;
    private int mLastRealStart = RecyclerView.NO_POSITION;
    private int mLastRealEnd = RecyclerView.NO_POSITION;
    private int mSlideStateStartPosition = RecyclerView.NO_POSITION;
    private boolean mHaveCalledSelectStart = false;
    /**
     * Developer callback which controls the behavior of DragSelectTouchHelper.
     */
    @NonNull
    private Callback mCallback;
    private Runnable mScrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsScrolling) {
                scrollBy(mScrollDistance);
                ViewCompat.postOnAnimation(mRecyclerView, mScrollRunnable);
            }
        }
    };
    private final RecyclerView.OnItemTouchListener mOnItemTouchListener = new RecyclerView.OnItemTouchListener() {
        @Override
        public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            Logger.d("onInterceptTouchEvent: x:" + e.getX() + ",y:" + e.getY()
                    + ", " + MotionEvent.actionToString(e.getAction()));
            RecyclerView.Adapter<?> adapter = rv.getAdapter();
            if (adapter == null || adapter.getItemCount() == 0) {
                return false;
            }
            boolean intercept = false;
            int action = e.getAction();
            int actionMask = action & MotionEvent.ACTION_MASK;
            // It seems that it's unnecessary to process multiple pointers.
            switch (actionMask) {
                case MotionEvent.ACTION_DOWN:
                    mDownY = e.getY();
                    // call the selection start's callback before moving
                    if (mSelectState == SELECT_STATE_SLIDE && isInSlideArea(e)) {
                        mSlideStateStartPosition = getItemPosition(rv, e);
                        if (mSlideStateStartPosition != RecyclerView.NO_POSITION) {
                            mCallback.onSelectStart(mSlideStateStartPosition);
                            mHaveCalledSelectStart = true;
                        }
                        intercept = true;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mSelectState == SELECT_STATE_DRAG_FROM_NORMAL
                            || mSelectState == SELECT_STATE_DRAG_FROM_SLIDE) {
                        Logger.i("onInterceptTouchEvent: drag mode move");
                        intercept = true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (mSelectState == SELECT_STATE_DRAG_FROM_NORMAL
                            || mSelectState == SELECT_STATE_DRAG_FROM_SLIDE) {
                        intercept = true;
                    }
                    // fall through
                case MotionEvent.ACTION_CANCEL:
                    // finger is lifted before moving
                    if (mSlideStateStartPosition != RecyclerView.NO_POSITION) {
                        selectFinished(mSlideStateStartPosition);
                        mSlideStateStartPosition = RecyclerView.NO_POSITION;
                    }
                    // selection has triggered
                    if (mStart != RecyclerView.NO_POSITION) {
                        selectFinished(mEnd);
                    }
                    break;
                default:
                    // do nothing
            }
            // Intercept only when the selection is triggered
            Logger.d("intercept result: " + intercept);
            return intercept;
        }

        @Override
        public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            if (!isActivated()) {
                return;
            }
            Logger.d("onTouchEvent: x:" + e.getX() + ",y:" + e.getY()
                    + ", " + MotionEvent.actionToString(e.getAction()));
            int action = e.getAction();
            int actionMask = action & MotionEvent.ACTION_MASK;
            switch (actionMask) {
                case MotionEvent.ACTION_MOVE:
                    if (mSlideStateStartPosition != RecyclerView.NO_POSITION) {
                        selectFirstItem(mSlideStateStartPosition);
                        // selection is triggered
                        mSlideStateStartPosition = RecyclerView.NO_POSITION;
                        Logger.i("onTouchEvent: after slide mode down");
                    }
                    processAutoScroll(e);
                    if (!mIsInTopHotspot && !mIsInBottomHotspot) {
                        updateSelectedRange(rv, e);
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    selectFinished(mEnd);
                    break;
                default:
                    // do nothing
            }
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            if (disallowIntercept) {
                inactiveSelect();
            }
        }
    };

    public DragSelectTouchHelper(@NonNull Callback callback) {
        mCallback = callback;
        mDisplayMetrics = Resources.getSystem().getDisplayMetrics();
        setHotspotRatio(DEFAULT_HOTSPOT_RATIO);
        setHotspotOffset(DEFAULT_HOTSPOT_OFFSET);
        setMaximumVelocity(DEFAULT_MAX_SCROLL_VELOCITY);
        setEdgeType(DEFAULT_EDGE_TYPE);
        setAutoEnterSlideState(false);
        setAllowDragInSlideState(false);
        setSlideArea(0, 0);
    }

    /**
     * Attaches the DragSelectTouchHelper to the provided RecyclerView. If TouchHelper is already
     * attached to a RecyclerView, it will first detach from the previous one. You can call this
     * method with {@code null} to detach it from the current RecyclerView.
     *
     * @param recyclerView The RecyclerView instance to which you want to add this helper or
     *                     {@code null} if you want to remove DragSelectTouchHelper from the
     *                     current RecyclerView.
     */
    public void attachToRecyclerView(@Nullable RecyclerView recyclerView) {
        if (mRecyclerView == recyclerView) {
            return; // nothing to do
        }
        if (mRecyclerView != null) {
            mRecyclerView.removeOnItemTouchListener(mOnItemTouchListener);
        }
        mRecyclerView = recyclerView;
        if (mRecyclerView != null) {
            mRecyclerView.addOnItemTouchListener(mOnItemTouchListener);
            mRecyclerView.addOnLayoutChangeListener(mOnLayoutChangeListener);
        }
    }

    /**
     * Activate the slide selection mode.
     */
    public void activeSlideSelect() {
        activeSelectInternal(RecyclerView.NO_POSITION);
    }

    /**
     * Activate the selection mode with selected item position. Normally called on long press.
     *
     * @param position Indicates the position of selected item.
     */
    public void activeDragSelect(int position) {
        activeSelectInternal(position);
    }

    /**
     * Exit the selection mode.
     */
    public void inactiveSelect() {
        if (isActivated()) {
            selectFinished(mEnd);
        } else {
            selectFinished(RecyclerView.NO_POSITION);
        }
        Logger.logSelectStateChange(mSelectState, SELECT_STATE_NORMAL);
        mSelectState = SELECT_STATE_NORMAL;
    }

    /**
     * To determine whether it is in the selection mode.
     *
     * @return true if is in the selection mode.
     */
    public boolean isActivated() {
        return (mSelectState != SELECT_STATE_NORMAL);
    }

    /**
     * Sets hotspot height by ratio of RecyclerView.
     *
     * @param ratio range (0, 0.5).
     * @return The select helper, which may used to chain setter calls.
     */
    public DragSelectTouchHelper setHotspotRatio(float ratio) {
        mHotspotHeightRatio = ratio;
        return this;
    }

    /**
     * Sets hotspot height.
     *
     * @param hotspotHeight hotspot height which unit is dp.
     * @return The select helper, which may used to chain setter calls.
     */
    public DragSelectTouchHelper setHotspotHeight(int hotspotHeight) {
        mHotspotHeight = dp2px(hotspotHeight);
        return this;
    }

    /**
     * Sets hotspot offset. It don't need to be set if no special requirement.
     *
     * @param hotspotOffset hotspot offset which unit is dp.
     * @return The select helper, which may used to chain setter calls.
     */
    public DragSelectTouchHelper setHotspotOffset(int hotspotOffset) {
        mHotspotOffset = dp2px(hotspotOffset);
        return this;
    }

    /**
     * Sets the activation edge type, one of:
     * <ul>
     * <li>{@link EdgeType#INSIDE} for edges that respond to touches inside
     * the bounds of the host view. If touch moves outside the bounds, scrolling
     * will stop.
     * <li>{@link EdgeType#INSIDE_EXTEND} for inside edges that continued to
     * scroll when touch moves outside the bounds of the host view.
     * </ul>
     *
     * @param type The type of edge to use.
     * @return The select helper, which may used to chain setter calls.
     */
    public DragSelectTouchHelper setEdgeType(EdgeType type) {
        switch (type) {
            case INSIDE:
                mScrollAboveTopRegion = false;
                mScrollBelowBottomRegion = false;
                break;
            case INSIDE_EXTEND:
            default:
                mScrollAboveTopRegion = true;
                mScrollBelowBottomRegion = true;
                break;
        }
        return this;
    }

    /**
     * Sets sliding area's start and end, has been considered RTL situation
     *
     * @param startDp The start of the sliding area
     * @param endDp   The end of the sliding area
     * @return The select helper, which may used to chain setter calls.
     */
    public DragSelectTouchHelper setSlideArea(int startDp, int endDp) {
        if (!isRtl()) {
            mSlideAreaLeft = dp2px(startDp);
            mSlideAreaRight = dp2px(endDp);
        } else {
            int displayWidth = mDisplayMetrics.widthPixels;
            mSlideAreaLeft = displayWidth - dp2px(endDp);
            mSlideAreaRight = displayWidth - dp2px(startDp);
        }
        return this;
    }

    /**
     * Sets the maximum velocity for scrolling
     *
     * @param velocity maximum velocity
     * @return The select helper, which may used to chain setter calls.
     */
    public DragSelectTouchHelper setMaximumVelocity(int velocity) {
        mMaximumVelocity = (int) (velocity * mDisplayMetrics.density + 0.5f);
        return this;
    }

    /**
     * Sets whether should auto enter slide mode after drag select finished.
     * It's usefully for LinearLayout RecyclerView.
     *
     * @param autoEnterSlideState should auto enter slide mode
     * @return The select helper, which may used to chain setter calls.
     */
    public DragSelectTouchHelper setAutoEnterSlideState(boolean autoEnterSlideState) {
        mShouldAutoChangeState = autoEnterSlideState;
        return this;
    }

    /**
     * Sets whether can drag selection in slide select mode.
     * It's usefully for LinearLayout RecyclerView.
     *
     * @param allowDragInSlideState allow drag selection in slide select mode
     * @return The select helper, which may used to chain setter calls.
     */
    public DragSelectTouchHelper setAllowDragInSlideState(boolean allowDragInSlideState) {
        mIsAllowDragInSlideState = allowDragInSlideState;
        return this;
    }

    private void init(int rvHeight) {
        if (mHotspotOffset >= rvHeight * MAX_HOTSPOT_RATIO) {
            mHotspotOffset = rvHeight * MAX_HOTSPOT_RATIO;
        }
        // The height of hotspot area is not set, using (RV height x ratio)
        if (mHotspotHeight <= 0) {
            if (mHotspotHeightRatio <= 0 || mHotspotHeightRatio >= MAX_HOTSPOT_RATIO) {
                mHotspotHeightRatio = DEFAULT_HOTSPOT_RATIO;
            }
            mHotspotHeight = rvHeight * mHotspotHeightRatio;
        } else {
            if (mHotspotHeight >= rvHeight * MAX_HOTSPOT_RATIO) {
                mHotspotHeight = rvHeight * MAX_HOTSPOT_RATIO;
            }
        }

        mTopRegionFrom = mHotspotOffset;
        mTopRegionTo = mTopRegionFrom + mHotspotHeight;
        mBottomRegionTo = rvHeight - mHotspotOffset;
        mBottomRegionFrom = mBottomRegionTo - mHotspotHeight;

        if (mTopRegionTo > mBottomRegionFrom) {
            mTopRegionTo = mBottomRegionFrom = rvHeight >> 1;
        }

        Logger.d("Hotspot: [" + mTopRegionFrom + ", " + mTopRegionTo + "], ["
                + mBottomRegionFrom + ", " + mBottomRegionTo + "]");
    }

    private void activeSelectInternal(int position) {
        // We should initialize the hotspot here, because its data may be delayed load
        if (mRecyclerView != null) {
            init(mRecyclerView.getHeight());
        }
        if (position == RecyclerView.NO_POSITION) {
            Logger.logSelectStateChange(mSelectState, SELECT_STATE_SLIDE);
            mSelectState = SELECT_STATE_SLIDE;
        } else {
            if (!mHaveCalledSelectStart) {
                mCallback.onSelectStart(position);
                mHaveCalledSelectStart = true;
            }
            if (mSelectState == SELECT_STATE_SLIDE) {
                if (mIsAllowDragInSlideState && selectFirstItem(position)) {
                    Logger.logSelectStateChange(mSelectState, SELECT_STATE_DRAG_FROM_SLIDE);
                    mSelectState = SELECT_STATE_DRAG_FROM_SLIDE;
                }
            } else if (mSelectState == SELECT_STATE_NORMAL) {
                if (selectFirstItem(position)) {
                    Logger.logSelectStateChange(mSelectState, SELECT_STATE_DRAG_FROM_NORMAL);
                    mSelectState = SELECT_STATE_DRAG_FROM_NORMAL;
                }
            } else {
                Logger.e("activeSelect in unexpected state: " + mSelectState);
            }
        }
    }

    private boolean selectFirstItem(int position) {
        boolean selectFirstItemSucceed = mCallback.onSelectChange(position, true);
        // The drag select feature is only available if the first item is available for selection
        if (selectFirstItemSucceed) {
            mStart = position;
            mEnd = position;
            mLastRealStart = position;
            mLastRealEnd = position;
        }
        return selectFirstItemSucceed;
    }

    private void updateSelectedRange(RecyclerView rv, MotionEvent e) {
        updateSelectedRange(rv, e.getX(), e.getY());
    }

    private void updateSelectedRange(RecyclerView rv, float x, float y) {
        int position = getItemPosition(rv, x, y);
        if (position != RecyclerView.NO_POSITION && mEnd != position) {
            mEnd = position;
            notifySelectRangeChange();
        }
    }

    private void notifySelectRangeChange() {
        if (mStart == RecyclerView.NO_POSITION || mEnd == RecyclerView.NO_POSITION) {
            return;
        }

        int newStart, newEnd;
        newStart = Math.min(mStart, mEnd);
        newEnd = Math.max(mStart, mEnd);
        if (mLastRealStart == RecyclerView.NO_POSITION || mLastRealEnd == RecyclerView.NO_POSITION) {
            if (newEnd - newStart == 1) {
                notifySelectChange(newStart, newStart, true);
            } else {
                notifySelectChange(newStart, newEnd, true);
            }
        } else {
            if (newStart > mLastRealStart) {
                notifySelectChange(mLastRealStart, newStart - 1, false);
            } else if (newStart < mLastRealStart) {
                notifySelectChange(newStart, mLastRealStart - 1, true);
            }

            if (newEnd > mLastRealEnd) {
                notifySelectChange(mLastRealEnd + 1, newEnd, true);
            } else if (newEnd < mLastRealEnd) {
                notifySelectChange(newEnd + 1, mLastRealEnd, false);
            }
        }

        mLastRealStart = newStart;
        mLastRealEnd = newEnd;
    }

    private void notifySelectChange(int start, int end, boolean newState) {
        for (int i = start; i <= end; i++) {
            mCallback.onSelectChange(i, newState);
        }
    }

    private void selectFinished(int lastItem) {
        if (lastItem != RecyclerView.NO_POSITION) {
            mCallback.onSelectEnd(lastItem);
        }
        mStart = RecyclerView.NO_POSITION;
        mEnd = RecyclerView.NO_POSITION;
        mLastRealStart = RecyclerView.NO_POSITION;
        mLastRealEnd = RecyclerView.NO_POSITION;
        mHaveCalledSelectStart = false;
        mIsInTopHotspot = false;
        mIsInBottomHotspot = false;
        stopAutoScroll();
        switch (mSelectState) {
            case SELECT_STATE_DRAG_FROM_NORMAL:
                if (mShouldAutoChangeState) {
                    Logger.logSelectStateChange(mSelectState, SELECT_STATE_SLIDE);
                    mSelectState = SELECT_STATE_SLIDE;
                } else {
                    Logger.logSelectStateChange(mSelectState, SELECT_STATE_NORMAL);
                    mSelectState = SELECT_STATE_NORMAL;
                }
                break;
            case SELECT_STATE_DRAG_FROM_SLIDE:
                Logger.logSelectStateChange(mSelectState, SELECT_STATE_SLIDE);
                mSelectState = SELECT_STATE_SLIDE;
                break;
            default:
                // doesn't change the selection state
                break;
        }
    }

    /**
     * Process motion event, according to the location to determine whether to scroll
     */
    private void processAutoScroll(MotionEvent e) {
        float y = e.getY();
        if (y >= mTopRegionFrom && y <= mTopRegionTo && y < mDownY) {
            mLastX = e.getX();
            mLastY = e.getY();
            float scrollDistanceFactor = (y - mTopRegionTo) / mHotspotHeight;
            mScrollDistance = (int) (mMaximumVelocity * scrollDistanceFactor);
            if (!mIsInTopHotspot) {
                mIsInTopHotspot = true;
                startAutoScroll();
                mDownY = mTopRegionTo;
            }
        } else if (mScrollAboveTopRegion && y < mTopRegionFrom && mIsInTopHotspot) {
            mLastX = e.getX();
            mLastY = mTopRegionFrom;
            // Use the maximum speed
            mScrollDistance = mMaximumVelocity * -1;
            startAutoScroll();
        } else if (y >= mBottomRegionFrom && y <= mBottomRegionTo && y > mDownY) {
            mLastX = e.getX();
            mLastY = e.getY();
            float scrollDistanceFactor = (y - mBottomRegionFrom) / mHotspotHeight;
            mScrollDistance = (int) (mMaximumVelocity * scrollDistanceFactor);
            if (!mIsInBottomHotspot) {
                mIsInBottomHotspot = true;
                startAutoScroll();
                mDownY = mBottomRegionFrom;
            }
        } else if (mScrollBelowBottomRegion && y > mBottomRegionTo && mIsInBottomHotspot) {
            mLastX = e.getX();
            mLastY = mBottomRegionTo;
            // Use the maximum speed
            mScrollDistance = mMaximumVelocity;
            startAutoScroll();
        } else {
            mIsInTopHotspot = false;
            mIsInBottomHotspot = false;
            mLastX = Float.MIN_VALUE;
            mLastY = Float.MIN_VALUE;
            stopAutoScroll();
        }

    }

    private void startAutoScroll() {
        if (!mIsScrolling) {
            mIsScrolling = true;
            mRecyclerView.removeCallbacks(mScrollRunnable);
            ViewCompat.postOnAnimation(mRecyclerView, mScrollRunnable);
        }
    }

    private void stopAutoScroll() {
        if (mIsScrolling) {
            mIsScrolling = false;
            mRecyclerView.removeCallbacks(mScrollRunnable);
        }
    }

    private void scrollBy(int distance) {
        int scrollDistance;
        if (distance > 0) {
            scrollDistance = Math.min(distance, mMaximumVelocity);
        } else {
            scrollDistance = Math.max(distance, -mMaximumVelocity);
        }
        mRecyclerView.scrollBy(0, scrollDistance);
        if (mLastX != Float.MIN_VALUE && mLastY != Float.MIN_VALUE) {
            updateSelectedRange(mRecyclerView, mLastX, mLastY);
        }
    }

    private int dp2px(float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, mDisplayMetrics);
    }

    private boolean isRtl() {
        return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                == View.LAYOUT_DIRECTION_RTL;
    }

    private boolean isInSlideArea(MotionEvent e) {
        float x = e.getX();
        return (x > mSlideAreaLeft && x < mSlideAreaRight);
    }

    private int getItemPosition(RecyclerView rv, MotionEvent e) {
        return getItemPosition(rv, e.getX(), e.getY());
    }

    private int getItemPosition(RecyclerView rv, float x, float y) {
        final View v = rv.findChildViewUnder(x, y);
        if (v == null) {
            RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();
            if (layoutManager instanceof GridLayoutManager) {
                int lastVisibleItemPosition = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
                int lastItemPosition = layoutManager.getItemCount() - 1;
                if (lastItemPosition == lastVisibleItemPosition) {
                    return lastItemPosition;
                }
            }
            return RecyclerView.NO_POSITION;
        }
        return rv.getChildAdapterPosition(v);
    }

    /**
     * Edge type that specifies an activation area starting at the view bounds and extending inward.
     */
    public enum EdgeType {
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
    public abstract static class Callback {
        /**
         * Called when changing item state.
         *
         * @param position   this item want to change the state to new state.
         * @param isSelected true if the position should be selected, false otherwise.
         * @return Whether to set the new state successfully.
         */
        public abstract boolean onSelectChange(int position, boolean isSelected);

        /**
         * Called when selection start.
         *
         * @param start the first selected item.
         */
        public void onSelectStart(int start) {

        }

        /**
         * Called when selection end.
         *
         * @param end the last selected item.
         */
        public void onSelectEnd(int end) {

        }
    }

    /**
     * An advance Callback which provide 4 useful selection modes {@link Mode}.
     * <p>
     * Note: Since the state of item may be repeatedly set, in order to improve efficiency,
     * please process it in the Adapter
     */
    public abstract static class AdvanceCallback<T> extends Callback {
        private Mode mMode;
        private Set<T> mOriginalSelection;
        private boolean mFirstWasSelected;

        /**
         * Creates a SimpleCallback with default {@link Mode#SelectAndReverse}# mode.
         *
         * @see Mode
         */
        public AdvanceCallback() {
            setMode(Mode.SelectAndReverse);
        }

        /**
         * Creates a SimpleCallback with select mode.
         *
         * @param mode the initial select mode
         * @see Mode
         */
        public AdvanceCallback(Mode mode) {
            setMode(mode);
        }

        /**
         * Sets the select mode.
         *
         * @param mode The type of select mode.
         * @see Mode
         */
        public void setMode(Mode mode) {
            mMode = mode;
        }

        @Override
        public void onSelectStart(int start) {
            mOriginalSelection = new HashSet<>();
            Set<T> selected = currentSelectedId();
            if (selected != null) {
                mOriginalSelection.addAll(selected);
            }
            mFirstWasSelected = mOriginalSelection.contains(getItemId(start));
        }

        @Override
        public void onSelectEnd(int end) {
            mOriginalSelection = null;
        }

        @Override
        public boolean onSelectChange(int position, boolean isSelected) {
            boolean stateChanged;
            switch (mMode) {
                case SelectAndKeep: {
                    stateChanged = updateSelectState(position, true);
                    break;
                }
                case SelectAndReverse: {
                    stateChanged = updateSelectState(position, isSelected);
                    break;
                }
                case SelectAndUndo: {
                    if (isSelected) {
                        stateChanged = updateSelectState(position, true);
                    } else {
                        stateChanged = updateSelectState(position, mOriginalSelection.contains(getItemId(position)));
                    }
                    break;
                }
                case ToggleAndKeep: {
                    stateChanged = updateSelectState(position, !mFirstWasSelected);
                    break;
                }
                case ToggleAndReverse: {
                    if (isSelected) {
                        stateChanged = updateSelectState(position, !mFirstWasSelected);
                    } else {
                        stateChanged = updateSelectState(position, mFirstWasSelected);
                    }
                    break;
                }
                case ToggleAndUndo: {
                    if (isSelected) {
                        stateChanged = updateSelectState(position, !mFirstWasSelected);
                    } else {
                        stateChanged = updateSelectState(position, mOriginalSelection.contains(getItemId(position)));
                    }
                    break;
                }
                default:
                    // SelectAndReverse Mode
                    stateChanged = updateSelectState(position, isSelected);
            }
            return stateChanged;
        }

        /**
         * Get the currently selected items when selecting first item.
         *
         * @return the currently selected item's id set.
         */
        public abstract Set<T> currentSelectedId();

        /**
         * Get the ID of the item.
         *
         * @param position item position to be judged.
         * @return item's identity.
         */
        public abstract T getItemId(int position);

        /**
         * Update the selection status of the position.
         *
         * @param position   the position who's selection state changed.
         * @param isSelected true if the position should be selected, false otherwise.
         * @return Whether to set the state successfully.
         */
        public abstract boolean updateSelectState(int position, boolean isSelected);

        /**
         * Different existing selection modes
         */
        public enum Mode {
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
            ToggleAndUndo,
        }
    }

    private static class Logger {
        private static boolean DEBUG = BuildConfig.DEBUG;

        private static void d(String msg) {
            if (DEBUG) {
                Log.d(TAG, msg);
            }
        }

        private static void e(String msg) {
            Log.e(TAG, msg);
        }

        private static void i(String msg) {
            Log.i(TAG, msg);
        }

        private static void logSelectStateChange(int before, int after) {
            i("Select state changed: " + stateName(before) + " --> " + stateName(after));
        }

        private static String stateName(int state) {
            switch (state) {
                case SELECT_STATE_NORMAL:
                    return "NormalState";
                case SELECT_STATE_SLIDE:
                    return "SlideState";
                case SELECT_STATE_DRAG_FROM_NORMAL:
                    return "DragFromNormal";
                case SELECT_STATE_DRAG_FROM_SLIDE:
                    return "DragFromSlide";
                default:
                    return "Unknown";
            }
        }
    }
}