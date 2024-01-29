package io.legado.app.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
inline fun <T> Flow<T>.onEachParallel(
    concurrency: Int = DEFAULT_CONCURRENCY,
    crossinline action: suspend (T) -> Unit
): Flow<T> = flatMapMerge(concurrency) { value ->
    return@flatMapMerge flow {
        action(value)
        emit(value)
    }
}
