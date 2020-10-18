package io.legado.app.help.coroutine

import io.legado.app.BuildConfig
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


@Suppress("unused")
class Coroutine<T>(
    val scope: CoroutineScope,
    context: CoroutineContext = Dispatchers.IO,
    block: suspend CoroutineScope.() -> T
) {

    companion object {

        val DEFAULT = MainScope()

        fun <T> async(
            scope: CoroutineScope = DEFAULT,
            context: CoroutineContext = Dispatchers.IO,
            block: suspend CoroutineScope.() -> T
        ): Coroutine<T> {
            return Coroutine(scope, context, block)
        }

    }

    private val job: Job

    private var start: VoidCallback? = null
    private var success: Callback<T>? = null
    private var error: Callback<Throwable>? = null
    private var finally: VoidCallback? = null
    private var cancel: VoidCallback? = null

    private var timeMillis: Long? = null
    private var errorReturn: Result<T>? = null

    val isCancelled: Boolean
        get() = job.isCancelled

    val isActive: Boolean
        get() = job.isActive

    val isCompleted: Boolean
        get() = job.isCompleted

    init {
        this.job = executeInternal(context, block)
    }

    fun timeout(timeMillis: () -> Long): Coroutine<T> {
        this.timeMillis = timeMillis()
        return this@Coroutine
    }

    fun timeout(timeMillis: Long): Coroutine<T> {
        this.timeMillis = timeMillis
        return this@Coroutine
    }

    fun onErrorReturn(value: () -> T?): Coroutine<T> {
        this.errorReturn = Result(value())
        return this@Coroutine
    }

    fun onErrorReturn(value: T?): Coroutine<T> {
        this.errorReturn = Result(value)
        return this@Coroutine
    }

    fun onStart(
        context: CoroutineContext? = null,
        block: (suspend CoroutineScope.() -> Unit)
    ): Coroutine<T> {
        this.start = VoidCallback(context, block)
        return this@Coroutine
    }

    fun onSuccess(
        context: CoroutineContext? = null,
        block: suspend CoroutineScope.(T) -> Unit
    ): Coroutine<T> {
        this.success = Callback(context, block)
        return this@Coroutine
    }

    fun onError(
        context: CoroutineContext? = null,
        block: suspend CoroutineScope.(Throwable) -> Unit
    ): Coroutine<T> {
        this.error = Callback(context, block)
        return this@Coroutine
    }

    fun onFinally(
        context: CoroutineContext? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Coroutine<T> {
        this.finally = VoidCallback(context, block)
        return this@Coroutine
    }

    fun onCancel(
        context: CoroutineContext? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Coroutine<T> {
        this.cancel = VoidCallback(context, block)
        return this@Coroutine
    }

    //取消当前任务
    fun cancel(cause: CancellationException? = null) {
        job.cancel(cause)
        cancel?.let {
            MainScope().launch {
                if (null == it.context) {
                    it.block.invoke(scope)
                } else {
                    withContext(scope.coroutineContext.plus(it.context)) {
                        it.block.invoke(this)
                    }
                }
            }
        }
    }

    fun invokeOnCompletion(handler: CompletionHandler): DisposableHandle {
        return job.invokeOnCompletion(handler)
    }

    private fun executeInternal(
        context: CoroutineContext,
        block: suspend CoroutineScope.() -> T
    ): Job {
        return scope.plus(Dispatchers.Main).launch {
            try {
                start?.let { dispatchVoidCallback(this, it) }
                val value = executeBlock(scope, context, timeMillis ?: 0L, block)
                if (isActive) {
                    success?.let { dispatchCallback(this, value, it) }
                }
            } catch (e: Throwable) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace()
                }
                val consume: Boolean = errorReturn?.value?.let { value ->
                    if (isActive) {
                        success?.let { dispatchCallback(this, value, it) }
                    }
                    true
                } ?: false

                if (!consume && isActive) {
                    error?.let { dispatchCallback(this, e, it) }
                }
            } finally {
                if (isActive) {
                    finally?.let { dispatchVoidCallback(this, it) }
                }
            }
        }
    }

    private suspend inline fun dispatchVoidCallback(scope: CoroutineScope, callback: VoidCallback) {
        if (null == callback.context) {
            callback.block.invoke(scope)
        } else {
            withContext(scope.coroutineContext.plus(callback.context)) {
                callback.block.invoke(this)
            }
        }
    }

    private suspend inline fun <R> dispatchCallback(
        scope: CoroutineScope,
        value: R,
        callback: Callback<R>
    ) {
        if (!scope.isActive) return
        if (null == callback.context) {
            callback.block.invoke(scope, value)
        } else {
            withContext(scope.coroutineContext.plus(callback.context)) {
                callback.block.invoke(this, value)
            }
        }
    }

    private suspend inline fun executeBlock(
        scope: CoroutineScope,
        context: CoroutineContext,
        timeMillis: Long,
        noinline block: suspend CoroutineScope.() -> T
    ): T {
        return withContext(scope.coroutineContext.plus(context)) {
            if (timeMillis > 0L) withTimeout(timeMillis) {
                block()
            } else block()
        }
    }

    private data class Result<out T>(val value: T?)

    private inner class VoidCallback(
        val context: CoroutineContext?,
        val block: suspend CoroutineScope.() -> Unit
    )

    private inner class Callback<VALUE>(
        val context: CoroutineContext?,
        val block: suspend CoroutineScope.(VALUE) -> Unit
    )
}
