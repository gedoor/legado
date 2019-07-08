package io.legado.app.help.coroutine

import kotlinx.coroutines.*

class Coroutine<T>(private val scope: CoroutineScope) {

    companion object {

        fun <T> with(scope: CoroutineScope): Coroutine<T> {
            return Coroutine(scope)
        }
    }

    private var start: (() -> Unit)? = null
    private var success: ((T?) -> Unit)? = null
    private var error: ((Throwable) -> Unit)? = null
    private var finally: (() -> Unit)? = null

    fun execute(domain: suspend CoroutineScope.() -> T): Coroutine<T> {
        scope.launch {
            tryCatch(
                {
                    start?.let { it() }


                    val result: T? = withContext(Dispatchers.IO) {
                        domain()
                    }

                    success?.let { it(result) }


                },
                { e ->
                    error?.let { it(e) }
                },
                {
                    finally?.let { it() }
                })
        }
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

}
