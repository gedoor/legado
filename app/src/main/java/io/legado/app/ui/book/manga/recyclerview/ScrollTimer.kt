package io.legado.app.ui.book.manga.recyclerview

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.legado.app.model.ReadManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScrollTimer(
    lifecycleOwner: LifecycleOwner,
    speed: MutableStateFlow<Int>,
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

    init {
        speed.onEach {
            onSpeedChanged(it)
        }.flowOn(Dispatchers.Default).launchIn(coroutineScope)
    }

    private fun onSpeedChanged(distance: Int) {
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