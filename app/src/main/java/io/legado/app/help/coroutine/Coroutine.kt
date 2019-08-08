package io.legado.app.help.coroutine

import kotlinx.coroutines.*


class Coroutine<T>(scope: CoroutineScope, block: suspend CoroutineScope.() -> T) {

    companion object {

        val DEFAULT = MainScope()

        fun <T> async(scope: CoroutineScope = DEFAULT, block: suspend CoroutineScope.() -> T): Coroutine<T> {
            return Coroutine(scope, block)
        }

    }

    private val job: Job

    private var start: (suspend CoroutineScope.() -> Unit)? = null
    private var execute: (suspend CoroutineScope.(T?) -> Unit)? = null
    private var success: (suspend CoroutineScope.(T?) -> Unit)? = null
    private var error: (suspend CoroutineScope.(Throwable) -> Unit)? = null
    private var finally: (suspend CoroutineScope.() -> Unit)? = null

    private var timeMillis: Long? = null

    private var errorReturn: Result<T>? = null

    val isCancelled: Boolean
        get() = job.isCancelled

    val isActive: Boolean
        get() = job.isActive

    val isCompleted: Boolean
        get() = job.isCompleted

    init {
        this.job = scope.plus(Dispatchers.Main).launch {
            executeInternal(this@launch, block)
        }
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

    fun onStart(start: (suspend CoroutineScope.() -> Unit)): Coroutine<T> {
        this.start = start
        return this@Coroutine
    }

    fun onExecute(execute: suspend CoroutineScope.(T?) -> Unit): Coroutine<T> {
        this.execute = execute
        return this@Coroutine
    }

    fun onSuccess(success: suspend CoroutineScope.(T?) -> Unit): Coroutine<T> {
        this.success = success
        return this@Coroutine
    }

    fun onError(error: suspend CoroutineScope.(Throwable) -> Unit): Coroutine<T> {
        this.error = error
        return this@Coroutine
    }

    fun onFinally(finally: suspend CoroutineScope.() -> Unit): Coroutine<T> {
        this.finally = finally
        return this@Coroutine
    }

    //取消当前任务
    fun cancel(cause: CancellationException? = null) {
        job.cancel(cause)
    }

    fun invokeOnCompletion(handler: CompletionHandler): DisposableHandle {
        return job.invokeOnCompletion(handler)
    }

    private suspend fun executeInternal(scope: CoroutineScope, block: suspend CoroutineScope.() -> T) {
        tryCatch(
            {
                start?.invoke(scope)
                val result = executeBlockIO(block, timeMillis ?: 0L)
                success?.invoke(scope, result)
            },
            { e ->
                val consume: Boolean = errorReturn?.value?.let { value ->
                    success?.invoke(scope, value)
                    true
                } ?: false

                if (!consume) {
                    error?.invoke(scope, e)
                }
            },
            {
                finally?.invoke(scope)
            })
    }

    private suspend fun executeBlockIO(block: suspend CoroutineScope.() -> T, timeMillis: Long): T? {
        val execution = withContext(Dispatchers.IO) {
            val result = block()
            execute?.invoke(this, result)
            result
        }
        return if (timeMillis > 0L) withTimeout(timeMillis) { execution } else execution
    }

    private suspend fun tryCatch(
        tryBlock: suspend () -> Unit,
        errorBlock: (suspend (Throwable) -> Unit)? = null,
        finallyBlock: (suspend () -> Unit)? = null
    ) {
        try {
            tryBlock()
        } catch (e: Throwable) {
            errorBlock?.invoke(e)
        } finally {
            finallyBlock?.invoke()
        }
    }

    private data class Result<out T>(val value: T?)
}
