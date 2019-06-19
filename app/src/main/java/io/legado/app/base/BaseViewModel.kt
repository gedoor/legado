package io.legado.app.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger

open class BaseViewModel(application: Application) : AndroidViewModel(application),
    CoroutineScope by MainScope(), AnkoLogger {

    private val launchManager: MutableList<Job> = mutableListOf()

    protected fun launchOnUI(
        tryBlock: suspend CoroutineScope.() -> Unit,
        errorBlock: (suspend CoroutineScope.(Throwable) -> Unit)? = null,//失败
        finallyBlock: (suspend CoroutineScope.() -> Unit)? = null//结束
    ) {
        launchOnUI {
            tryCatch(tryBlock, errorBlock, finallyBlock)
        }
    }

    /**
     * add launch task to [launchManager]
     */
    private fun launchOnUI(block: suspend CoroutineScope.() -> Unit) {
        val job = launch { block() }//主线程
        launchManager.add(job)
        job.invokeOnCompletion { launchManager.remove(job) }
    }

    private suspend fun tryCatch(
        tryBlock: suspend CoroutineScope.() -> Unit,
        errorBlock: (suspend CoroutineScope.(Throwable) -> Unit)? = null,
        finallyBlock: (suspend CoroutineScope.() -> Unit)? = null
    ) {
        try {
            coroutineScope { tryBlock() }
        } catch (e: Throwable) {
            coroutineScope { errorBlock?.let { it(e) } }
        } finally {
            coroutineScope { finallyBlock?.let { it() } }
        }
    }

    override fun onCleared() {
        super.onCleared()
        launchManager.clear()
    }
}