package io.legado.app.ui.book.manga.recyclerview

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE

class ScrollTimer(
    private val callback: ScrollCallback,
    private val recyclerView: RecyclerView
) : RecyclerView.OnScrollListener() {
    private var distance = 1
    var isEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    recyclerView.addOnScrollListener(this)
                    startScroll()
                } else {
                    recyclerView.removeOnScrollListener(this)
                }
            }
        }

    override fun onScrollStateChanged(
        recyclerView: RecyclerView,
        newState: Int
    ) {
        if (newState == SCROLL_STATE_IDLE) {
            startScroll()
        }
    }

    fun setSpeed(distance: Int) {
        this.distance = distance
    }

    private fun startScroll() {
        callback.scrollBy(distance)
    }

    interface ScrollCallback {
        fun scrollBy(distance: Int)
    }
}