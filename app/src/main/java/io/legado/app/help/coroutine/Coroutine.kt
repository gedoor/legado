package io.legado.app.help.coroutine

import kotlinx.coroutines.*

class Coroutine<T>(private val domain: (suspend CoroutineScope.() -> T)? = null) : CoroutineScope by MainScope() {

    companion object {

        fun <T> of(value: suspend CoroutineScope.() -> T): Coroutine<T> {
            return Coroutine(value)
        }
    }

    private var start: (() -> Unit)? = null
    private var success: ((T?) -> Unit)? = null
    private var error: ((Throwable) -> Unit)? = null
    private var finally: (() -> Unit)? = null

    private var value: T? = null

    init {
        val job: Job = launch {
            tryCatch(
                {
                    start?.let { it() }


                    val result: T? = withContext(Dispatchers.IO) {
                        domain?.let {
                            value = it()
                            return@let value
                        }
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
    }

    fun onStart(start: (() -> Unit)): Coroutine<T> {
        this.start = start
        return this@Coroutine
    }
//
//    fun <U> map(func: Function<T, U>): Coroutine<U> {
//        return of { func.apply(value) }
//    }
//
//    fun <U> flatMap(func: Function<T, Coroutine<U>>): Coroutine<U> {
//        return func.apply(value)
//    }

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
