package io.legado.app.ui.book.manga.recyclerview

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScrollTimer(
    private val callback: ScrollCallback,
    private val recyclerView: RecyclerView,
    private val coroutine: LifecycleCoroutineScope,
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
        mScrollPageJob = coroutine.launch(Dispatchers.Default) {
            while (isActive) {
                delay(distance.times(1000L))
                withContext(Dispatchers.Main) {
                    callback.scrollPage()
                }
            }
        }
    }

    interface ScrollCallback {
        fun scrollBy(distance: Int)
        fun scrollPage()
    }
}