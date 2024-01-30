package io.legado.app.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <T> Flow<T>.onEachParallel(
    concurrency: Int,
    crossinline action: suspend (T) -> Unit
): Flow<T> = flatMapMerge(concurrency) { value ->
    return@flatMapMerge flow {
        action(value)
        emit(value)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <T, R> Flow<T>.mapParallel(
    concurrency: Int,
    crossinline transform: suspend (T) -> R,
): Flow<R> = flatMapMerge(concurrency) { value -> flow { emit(transform(value)) } }

inline fun <T, R> Flow<T>.mapNotNullParallel(
    concurrency: Int,
    crossinline transform: suspend (T) -> R?,
): Flow<R> = mapParallel(concurrency, transform).filterNotNull()

inline fun <T> Flow<T>.onEachIndexed(
    crossinline action: suspend (index: Int, T) -> Unit,
): Flow<T> = flow {
    var index = 0
    collect { value ->
        action(index++, value)
        emit(value)
    }
}
