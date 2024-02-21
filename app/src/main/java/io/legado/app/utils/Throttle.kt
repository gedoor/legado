package io.legado.app.utils

@Suppress("unused")
class Throttle<T>(
    wait: Long = 0L,
    leading: Boolean = true,
    trailing: Boolean = true,
    func: () -> T
) : Debounce<T>(wait, wait, leading, trailing, func)

fun <T> throttle(
    wait: Long = 0L,
    leading: Boolean = true,
    trailing: Boolean = true,
    func: () -> T
) = Throttle(wait, leading, trailing, func)
