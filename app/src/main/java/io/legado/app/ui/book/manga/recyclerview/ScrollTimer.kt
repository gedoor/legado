package io.legado.app.ui.book.manga.recyclerview

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScrollTimer(
    private val callback: ScrollCallback,
    private val recyclerView: RecyclerView,
    private val coroutineScope: CoroutineScope,
) : RecyclerView.OnScrollListener() {
    private var distance = 1
    private var mScrollPageJob: Job? = null
    var isEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    recyclerView.addOnScrollListener(this)
                    startScroll()
                } else {
                    recyclerView.removeOnScrollListener(this)
                    recyclerView.stopScroll()
                }
            }
        }

    var isEnabledPage: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    mScrollPageJob?.cancel()
                    startScrollPage()
                } else {
                    mScrollPageJob?.cancel()
                }
            }
        }

    override fun onScrollStateChanged(
        recyclerView: RecyclerView,
        newState: Int,
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

    private fun startScrollPage() {
        mScrollPageJob = coroutineScope.launch {
            while (isActive) {
                delay(distance.times(1000L))
                callback.scrollPage()
            }
        }
    }

    interface ScrollCallback {
        fun scrollBy(distance: Int)
        fun scrollPage()
    }
}