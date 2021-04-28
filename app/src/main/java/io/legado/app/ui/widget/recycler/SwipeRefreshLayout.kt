package io.legado.app.ui.widget.recycler

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.view.*

class SwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs), NestedScrollingParent3,
    NestedScrollingParent2, NestedScrollingChild3, NestedScrollingChild2, NestedScrollingParent,
    NestedScrollingChild {

    init {
        
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        TODO("Not yet implemented")
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun stopNestedScroll(type: Int) {
        TODO("Not yet implemented")
    }

    override fun hasNestedScrollingParent(type: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int,
        consumed: IntArray
    ) {
        TODO("Not yet implemented")
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        TODO("Not yet implemented")
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        TODO("Not yet implemented")
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        TODO("Not yet implemented")
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        TODO("Not yet implemented")
    }


}