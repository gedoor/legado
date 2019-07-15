package io.legado.app.help.coroutine

import kotlinx.coroutines.*

class Coroutine<T>(private val scope: CoroutineScope, private val block: suspend CoroutineScope.() -> T) {

    companion object {

        private val DEFAULT = MainScope()

        fun <T> launch(scope: CoroutineScope = DEFAULT, block: suspend CoroutineScope.() -> T): Coroutine<T> {
            return Coroutine(scope, block)
        }
    }

    init {
        scope.launch {
            executeInternal(block)
        }
    }

    private var start: (() -> Unit)? = null
    private var success: ((T?) -> Unit)? = null
    private var error: ((Throwable) -> Unit)? = null
    private var finally: (() -> Unit)? = null

    private var timeMillis: Long? = null

    private var errorReturn: Result<T>? = null

    fun timeout(timeMillis: () -> Long): Coroutine<T> {
        this.timeMillis = timeMillis()
        return this@Coroutine
    }

    fun onErrorReturn(value: () -> T?): Coroutine<T> {
        errorReturn = Result(value())
        return this@Coroutine
    }

    fun onStart(start: (() -> Unit)): Coroutine<T> {
        this.start = start
        return this@Coroutine
    }

    fun onSuccess(success: (T?) -> Unit): Coroutine<T> {
        this.success = success
        return this@Coroutine
    }

    fun onError(error: (Throwable) -> Unit): Coroutine<T> {
        this.error = error
        return this@Coroutine
    }

    fun onFinally(finally: () -> Unit): Coroutine<T> {
        this.finally = finally
        return this@Coroutine
    }

    fun cancel(cause: CancellationException? = null) {
        scope.cancel(cause)
    }

    private suspend fun executeInternal(block: suspend CoroutineScope.() -> T) {
        tryCatch(
            {
                start?.let { it() }

                val time = timeMillis
                val result = if (time != null && time > 0) {
                    withTimeout(time) {
                        executeBlock(block)
                    }
                } else {
                    executeBlock(block)
                }

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

    private suspend fun executeBlock(block: suspend CoroutineScope.() -> T): T? {
        return withContext(Dispatchers.IO) {
            block()
        }
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
