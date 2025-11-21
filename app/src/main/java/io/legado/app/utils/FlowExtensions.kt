package io.legado.app.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.legado.app.data.appDb
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.sync.Semaphore

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
            currentCoroutineContext().ensureActive()
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
        } catch (_: Throwable) {
            currentCoroutineContext().ensureActive()
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
            currentCoroutineContext().ensureActive()
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

fun <T> Flow<T>.flowWithLifecycleFirst(
    lifecycle: Lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED
): Flow<T> = callbackFlow {
    if (!lifecycle.currentState.isAtLeast(minActiveState)) {
        firstOrNull()?.let {
            send(it)
        }
    }
    lifecycle.repeatOnLifecycle(minActiveState) {
        this@flowWithLifecycleFirst.collect {
            send(it)
        }
    }
    close()
}

fun <T> Flow<T>.flowWithLifecycleAndDatabaseChange(
    lifecycle: Lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    table: String
): Flow<T> = callbackFlow {
    var update = 0
    val channel = appDb.invalidationTracker
        .createFlow(table)
        .conflate()
        .onEach { update++ }
        .produceIn(this)
    lifecycle.repeatOnLifecycle(minActiveState) {
        if (update == 0) {
            channel.receive()
        }
        this@flowWithLifecycleAndDatabaseChange.collect {
            update = 0
            send(it)
        }
    }
    close()
}

fun <T> Flow<T>.flowWithLifecycleAndDatabaseChangeFirst(
    lifecycle: Lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    table: String
): Flow<T> = callbackFlow {
    var update = 0
    val isActive = lifecycle.currentState.isAtLeast(minActiveState)
    val channel = appDb.invalidationTracker
        .createFlow(table, emitInitialState = isActive)
        .conflate()
        .onEach { update++ }
        .produceIn(this)
    if (!isActive) {
        firstOrNull()?.let {
            send(it)
        }
    }
    lifecycle.repeatOnLifecycle(minActiveState) {
        if (update == 0) {
            channel.receive()
        }
        this@flowWithLifecycleAndDatabaseChangeFirst.collect {
            update = 0
            send(it)
        }
    }
    close()
}
