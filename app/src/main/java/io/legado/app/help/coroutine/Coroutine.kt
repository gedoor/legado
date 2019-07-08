package io.legado.app.help.coroutine

import android.util.Log
import kotlinx.coroutines.*

class Coroutine<T>(scope: CoroutineScope, private val domain: suspend CoroutineScope.() -> T) {

    companion object {

        fun <T> with(scope: CoroutineScope, domain: suspend CoroutineScope.() -> T): Coroutine<T> {
            return Coroutine(scope, domain)
        }
    }

    init {
        scope.launch {
            executeInternal(domain)
        }
    }

    private var start: (() -> Unit)? = null
    private var success: ((T?) -> Unit)? = null
    private var error: ((Throwable) -> Unit)? = null
    private var finally: (() -> Unit)? = null

    private var timeMillis: Long? = null

    fun timeout(timeMillis: Long): Coroutine<T> {
        this.timeMillis = timeMillis
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

    private suspend fun executeInternal(domain: suspend CoroutineScope.() -> T) {
        tryCatch(
            {
                start?.let { it() }

                val result = if (timeMillis != null && timeMillis!! > 0) {
                    withTimeout(timeMillis!!) {
                        executeDomain(domain)
                    }
                } else {
                    executeDomain(domain)
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

    private suspend fun executeDomain(domain: suspend CoroutineScope.() -> T): T? {
        return withContext(Dispatchers.IO) {
            Log.e("TAG!", "executeDomain")
            domain()
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

}
