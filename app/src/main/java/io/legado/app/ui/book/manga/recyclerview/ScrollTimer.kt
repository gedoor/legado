package io.legado.app.ui.book.manga.recyclerview

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScrollTimer(
    private val coroutineScope: CoroutineScope,
    private val callback: ScrollCallback
) {
    private var job: Job? = null
    private var delayMs: Long = 20L
    private var distance = 1
    var isEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                restartJob()
            }
        }

    fun setSpeed(distance: Int) {
        this.distance = distance
        restartJob()
    }

    private fun restartJob() {
        job?.cancel()
        if (!isEnabled || delayMs == 0L) {
            job = null
            return
        }
        job = coroutineScope.launch {
            while (isActive) {
                callback.scrollBy(distance)
                delay(delayMs)
            }
        }
    }

    interface ScrollCallback{
        fun scrollBy(distance:Int)
    }
}