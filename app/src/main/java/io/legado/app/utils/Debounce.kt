package io.legado.app.utils

import android.os.SystemClock
import kotlin.math.max

@Suppress("MemberVisibilityCanBePrivate")
open class Debounce<T>(
    var wait: Long = 0L,
    var maxWait: Long = -1L,
    var leading: Boolean = false,
    var trailing: Boolean = true,
    private val func: () -> T
) {
    companion object {
        private val handler by lazy { buildMainHandler() }
    }

    private var lastCallTime = -1L
    private var lastInvokeTime = 0L
    private val maxing get() = maxWait != -1L
    private var result: T? = null
    private var hasTimer = false
    private val timerExpiredRunnable = Runnable {
        timerExpired()
    }

    init {
        maxWait = if (maxing) max(maxWait, wait) else maxWait
    }

    private fun invokeFunc(time: Long): T {
        lastInvokeTime = time
        return func.invoke().also { result = it }
    }

    private fun startTimer(wait: Long) {
        hasTimer = true
        handler.postDelayed(timerExpiredRunnable, wait)
    }

    private fun cancelTimer() {
        handler.removeCallbacks(timerExpiredRunnable)
    }

    private fun leadingEdge(time: Long): T? {
        lastInvokeTime = time
        startTimer(wait)
        return if (leading) invokeFunc(time) else result
    }

    private fun trailingEdge(time: Long): T? {
        hasTimer = false
        return if (trailing) invokeFunc(time) else result
    }

    private fun remainingWait(time: Long): Long {
        val timeSinceLastCall = time - lastCallTime
        val timeSinceLastInvoke = time - lastInvokeTime
        val timeWaiting = wait - timeSinceLastCall

        return if (maxing) timeWaiting.coerceAtMost(maxWait - timeSinceLastInvoke) else timeWaiting
    }

    private fun shouldInvoke(time: Long): Boolean {
        val timeSinceLastCall = time - lastCallTime
        val timeSinceLastInvoke = time - lastInvokeTime

        return lastCallTime == -1L
                || timeSinceLastCall >= wait
                || timeSinceLastCall < 0
                || maxing && timeSinceLastInvoke >= maxWait
    }

    private fun timerExpired() {
        val time = SystemClock.uptimeMillis()
        if (shouldInvoke(time)) {
            trailingEdge(time)
        } else {
            startTimer(remainingWait(time))
        }
    }

    fun cancel() {
        if (hasTimer) {
            cancelTimer()
        }
        lastInvokeTime = 0
        lastCallTime = -1L
        hasTimer = false
    }

    fun flush(): T? {
        return if (hasTimer) trailingEdge(SystemClock.uptimeMillis()) else result
    }

    fun pending(): Boolean = hasTimer

    operator fun invoke(): T? {
        val time = SystemClock.uptimeMillis()
        val isInvoking = shouldInvoke(time)

        lastCallTime = time

        if (isInvoking) {
            if (!hasTimer) {
                return leadingEdge(lastCallTime)
            }
            if (maxing) {
                startTimer(wait)
                return invokeFunc(lastCallTime)
            }
        }

        if (!hasTimer) {
            startTimer(wait)
        }

        return result
    }

}

fun <T> debounce(
    wait: Long = 0L,
    maxWait: Long = -1L,
    leading: Boolean = false,
    trailing: Boolean = true,
    func: () -> T
) = Debounce(wait, maxWait, leading, trailing, func)
