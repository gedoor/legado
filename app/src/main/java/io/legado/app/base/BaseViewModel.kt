package io.legado.app.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

open class BaseViewModel(application: Application) : AndroidViewModel(application), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main


    private val launchManager: MutableList<Job> = mutableListOf()

    protected fun launchOnUI(
        tryBlock: suspend CoroutineScope.() -> Unit,
        cacheBlock: suspend CoroutineScope.(Throwable) -> Unit,
        finallyBlock: suspend CoroutineScope.() -> Unit,
        handleCancellationExceptionManually: Boolean
    ) {
        launchOnUI {
            tryCatch(tryBlock, cacheBlock, finallyBlock, handleCancellationExceptionManually)
        }
    }

    /**
     * add launch task to [launchManager]
     */
    private fun launchOnUI(block: suspend CoroutineScope.() -> Unit) {
        val job = launch { block() }
        launchManager.add(job)
        job.invokeOnCompletion { launchManager.remove(job) }
    }

    private suspend fun tryCatch(
        tryBlock: suspend CoroutineScope.() -> Unit,
        catchBlock: suspend CoroutineScope.(Throwable) -> Unit,
        finallyBlock: suspend CoroutineScope.() -> Unit,
        handleCancellationExceptionManually: Boolean = false
    ) {
        try {
            coroutineScope { tryBlock() }
        } catch (e: Throwable) {
            if (e !is CancellationException || handleCancellationExceptionManually) {
                coroutineScope { catchBlock(e) }
            } else {
                throw e
            }
        } finally {
            coroutineScope { finallyBlock() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        launchManager.clear()
    }
}