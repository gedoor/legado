package io.legado.app.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Semaphore
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <T> Flow<T>.onEachParallel(
    concurrency: Int,
    crossinline action: suspend (T) -> Unit
): Flow<T> = flatMapMerge(concurrency) { value ->
    flow {
        action(value)
        emit(value)
    }
}.buffer(0)

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <T> Flow<T>.onEachParallelSafe(
    concurrency: Int,
    crossinline action: suspend (T) -> Unit
): Flow<T> = flatMapMerge(concurrency) { value ->
    flow {
        try {
            action(value)
        } catch (e: Throwable) {
            coroutineContext.ensureActive()
        }
        emit(value)
    }
}.buffer(0)

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <T, R> Flow<T>.mapParallel(
    concurrency: Int,
    crossinline transform: suspend (T) -> R,
): Flow<R> = flatMapMerge(concurrency) { value -> flow { emit(transform(value)) } }.buffer(0)


@OptIn(ExperimentalCoroutinesApi::class)
inline fun <T, R> Flow<T>.mapParallelSafe(
    concurrency: Int,
    crossinline transform: suspend (T) -> R,
): Flow<R> = flatMapMerge(concurrency) { value ->
    flow {
        try {
            emit(transform(value))
        } catch (e: Throwable) {
            coroutineContext.ensureActive()
        }
    }
}.buffer(0)

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <T, R> Flow<T>.transformParallelSafe(
    concurrency: Int,
    crossinline transform: suspend FlowCollector<R>.(T) -> R,
): Flow<R> = flatMapMerge(concurrency) { value ->
    flow {
        try {
            transform(value)
        } catch (e: Throwable) {
            coroutineContext.ensureActive()
        }
    }
}.buffer(0)

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

inline fun <T, R> Flow<T>.mapIndexed(
    crossinline action: suspend (index: Int, T) -> R,
): Flow<R> = flow {
    var index = 0
    collect { value ->
        emit(action(index++, value))
    }
}

inline fun <T, R> Flow<T>.mapAsync(
    concurrency: Int,
    crossinline transform: suspend (T) -> R
): Flow<R> = if (concurrency == 1) {
    map { transform(it) }
} else {
    Semaphore(concurrency).let { semaphore ->
        channelFlow {
            collect {
                semaphore.acquire()
                send(async { transform(it) })
            }
        }.map {
            it.await()
        }.onEach { semaphore.release() }
    }.buffer(0)
}

inline fun <T, R> Flow<T>.mapAsyncIndexed(
    concurrency: Int,
    crossinline transform: suspend (index: Int, T) -> R
): Flow<R> = if (concurrency == 1) {
    mapIndexed { index, value ->
        transform(index, value)
    }
} else {
    Semaphore(concurrency).let { semaphore ->
        channelFlow {
            var index = 0
            collect {
                semaphore.acquire()
                val i = index++
                send(async { transform(i, it) })
            }
        }.map {
            it.await()
        }.onEach { semaphore.release() }
    }.buffer(0)
}

inline fun <T> Flow<T>.onEachAsync(
    concurrency: Int,
    crossinline action: suspend (T) -> Unit
): Flow<T> = if (concurrency == 1) {
    onEach { action(it) }
} else {
    Semaphore(concurrency).let { semaphore ->
        channelFlow {
            collect {
                semaphore.acquire()
                send(async {
                    action(it)
                    it
                })
            }
        }.map {
            it.await()
        }.onEach { semaphore.release() }
    }.buffer(0)
}

inline fun <T> Flow<T>.onEachAsyncIndexed(
    concurrency: Int,
    crossinline action: suspend (index: Int, T) -> Unit
): Flow<T> = if (concurrency == 1) {
    onEachIndexed { index, value ->
        action(index, value)
    }
} else {
    Semaphore(concurrency).let { semaphore ->
        channelFlow {
            var index = 0
            collect {
                semaphore.acquire()
                val i = index++
                send(async {
                    action(i, it)
                    it
                })
            }
        }.map {
            it.await()
        }.onEach { semaphore.release() }
    }.buffer(0)
}
