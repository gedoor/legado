package io.legado.app.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import kotlin.coroutines.CoroutineContext

open class BaseViewModel(application: Application) : AndroidViewModel(application), CoroutineScope, AnkoLogger {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main


    private val launchManager: MutableList<Job> = mutableListOf()

    protected fun launchOnUI(
        tryBlock: suspend CoroutineScope.() -> Unit,//成功
        errorBlock: suspend CoroutineScope.(Throwable) -> Unit,//失败
        finallyBlock: suspend CoroutineScope.() -> Unit//结束
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
        errorBlock: suspend CoroutineScope.(Throwable) -> Unit,
        finallyBlock: suspend CoroutineScope.() -> Unit
    ) {
        try {
            coroutineScope { tryBlock() }
        } catch (e: Throwable) {
            coroutineScope { errorBlock(e) }
        } finally {
            coroutineScope { finallyBlock() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        launchManager.clear()
    }
}