package io.legado.app.help.coroutine

import kotlinx.coroutines.*


class Coroutine<T>() {

    companion object {

        private val DEFAULT = MainScope()

        fun <T> async(scope: CoroutineScope = DEFAULT, block: suspend CoroutineScope.() -> T): Coroutine<T> {
            return Coroutine(scope, block)
        }

        fun <T> plus(coroutine: Coroutine<T>): Coroutine<T> {
            return Coroutine(coroutine)
        }
    }

    private var interceptor: Coroutine<T>? = null
    private var job: Job? = null

    private var start: (suspend CoroutineScope.() -> Unit)? = null
    private var success: (suspend CoroutineScope.(T?) -> Unit)? = null
    private var error: (suspend CoroutineScope.(Throwable) -> Unit)? = null
    private var finally: (suspend CoroutineScope.() -> Unit)? = null

    private var timeMillis: Long? = null

    private var errorReturn: Result<T>? = null

    private constructor(
        scope: CoroutineScope,
        block: suspend CoroutineScope.() -> T
    ) : this() {
        this.job = scope.launch {
            executeInternal(block)
        }
    }

    private constructor(coroutine: Coroutine<T>) : this() {
        this.interceptor = coroutine
        this.job = coroutine.job
    }

    fun timeout(timeMillis: () -> Long): Coroutine<T> {
        if (this.interceptor != null) {
            this.interceptor!!.timeMillis = timeMillis()
        } else {
            this.timeMillis = timeMillis()
        }
        return this@Coroutine
    }

    fun onErrorReturn(value: () -> T?): Coroutine<T> {
        if (this.interceptor != null) {
            this.interceptor!!.errorReturn = Result(value())
        } else {
            errorReturn = Result(value())
        }
        return this@Coroutine
    }

    fun onStart(start: (suspend CoroutineScope.() -> Unit)): Coroutine<T> {
        if (this.interceptor != null) {
            this.interceptor!!.start = start
        } else {
            this.start = start
        }
        return this@Coroutine
    }

    fun onSuccess(success: suspend CoroutineScope.(T?) -> Unit): Coroutine<T> {
        if (this.interceptor != null) {
            this.interceptor!!.success = success
        } else {
            this.success = success
        }
        return this@Coroutine
    }

    fun onError(error: suspend CoroutineScope.(Throwable) -> Unit): Coroutine<T> {
        if (this.interceptor != null) {
            this.interceptor!!.error = error
        } else {
            this.error = error
        }
        return this@Coroutine
    }

    fun onFinally(finally: suspend CoroutineScope.() -> Unit): Coroutine<T> {
        if (this.interceptor != null) {
            this.interceptor!!.finally = finally
        } else {
            this.finally = finally
        }
        return this@Coroutine
    }

    //取消当前任务
    fun cancel(cause: CancellationException? = null) {
        job?.cancel(cause)
    }

    private suspend fun executeInternal(block: suspend CoroutineScope.() -> T) {
        tryCatch(
            {
                start?.let { it() }

                val result = executeBlock(block, timeMillis ?: 0L)

                success?.let { it(result) }
            },
            { e ->
                val consume: Boolean = errorReturn?.value?.let { value ->
                    success?.let { it(value) }
                    true
                } ?: false

                if (!consume) {
                    error?.let { it(e) }
                }
            },
            {
                finally?.let { it() }
            })
    }

    private suspend fun executeBlock(block: suspend CoroutineScope.() -> T, timeMillis: Long): T? {
        val asyncBlock = withContext(Dispatchers.IO) {
            block()
        }
        return if (timeMillis > 0L) withTimeout(timeMillis) { asyncBlock } else asyncBlock
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

    private data class Result<out T>(val value: T?)
}
