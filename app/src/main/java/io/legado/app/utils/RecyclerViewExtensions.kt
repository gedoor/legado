package io.legado.app.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

fun RecyclerView.findCenterViewPosition(atFirstPosition: Boolean, atLastPosition: Boolean): Int {
    val centerView = findChildViewUnder(width / 2f, height / 2f)
    if (centerView != null) {
        return getChildAdapterPosition(centerView)
    }
    val lm = layoutManager as? LinearLayoutManager ?: return RecyclerView.NO_POSITION
    return when {
        atLastPosition -> lm.findLastVisibleItemPosition()
        atFirstPosition -> lm.findFirstVisibleItemPosition()
        else -> {
            val visiblePositions =
                (lm.findFirstVisibleItemPosition()..lm.findLastVisibleItemPosition())
            if (visiblePositions.isEmpty()) RecyclerView.NO_POSITION
            else visiblePositions.minByOrNull {
                abs(it - (lm.itemCount / 2))
            } ?: RecyclerView.NO_POSITION
        }
    }
}

fun RecyclerView.findViewPosition(x: Float, y: Float): Int {
    return getChildAdapterPosition(findChildViewUnder(x, y) ?: return RecyclerView.NO_POSITION)
}

fun RecyclerView.findFirstVisibleViewPosition(): Int {
    var pos = -1
    if (layoutManager is LinearLayoutManager) {
        pos = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
    }
    return pos
}

fun RecyclerView.findLastVisibleViewPosition(): Int {
    var pos = -1
    if (layoutManager is LinearLayoutManager) {
        pos = (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
    }
    return pos
}