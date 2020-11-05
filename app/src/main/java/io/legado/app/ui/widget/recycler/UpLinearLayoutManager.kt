package io.legado.app.ui.widget.recycler

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller

@Suppress("MemberVisibilityCanBePrivate", "unused")
class UpLinearLayoutManager(val context: Context) : LinearLayoutManager(context) {

    fun smoothScrollToPosition(position: Int) {
        smoothScrollToPosition(position, 0)
    }

    fun smoothScrollToPosition(position: Int, offset: Int) {
        val scroller = UpLinearSmoothScroller(context)
        scroller.targetPosition = position
        scroller.offset = offset
        startSmoothScroll(scroller)
    }
}

class UpLinearSmoothScroller(context: Context?): LinearSmoothScroller(context) {
    var offset = 0

    override fun getVerticalSnapPreference(): Int {
        return SNAP_TO_START
    }

    override fun getHorizontalSnapPreference(): Int {
        return SNAP_TO_START
    }

    override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
        if (snapPreference == SNAP_TO_START) {
            return boxStart - viewStart + offset
        }
        throw IllegalArgumentException("snap preference should be SNAP_TO_START")
    }
}