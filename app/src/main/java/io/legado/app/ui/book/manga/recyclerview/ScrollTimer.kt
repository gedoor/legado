package io.legado.app.ui.book.manga.recyclerview

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.legado.app.model.ReadManga
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScrollTimer(
    lifecycleOwner: LifecycleOwner,
) {
    private val coroutineScope = lifecycleOwner.lifecycleScope
    private var job: Job? = null
    private var delayMs: Long = 16L
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
                delay(delayMs)
                ReadManga.mCallback?.scrollBy(distance)
            }
        }
    }

}