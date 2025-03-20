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
import kotlin.math.roundToLong

private const val MAX_DELAY = 8L

class ScrollTimer(
    lifecycleOwner: LifecycleOwner,
    speed: MutableStateFlow<Float>,
) {
    private val coroutineScope = lifecycleOwner.lifecycleScope
    private var job: Job? = null
    private var delayMs: Long = 10L

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

    private fun onSpeedChanged(speed: Float) {
        var initSpeed = speed
        if (initSpeed <= 0f) {
            delayMs = 0L
        }
        if (initSpeed > 1f) {
            initSpeed = 0.99f
        }
        val speedFactor = 1 - initSpeed
        delayMs = (MAX_DELAY * speedFactor).roundToLong()
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
                delay((delayMs).toLong())
                ReadManga.mCallback?.scrollBy(1)
            }
        }
    }

}