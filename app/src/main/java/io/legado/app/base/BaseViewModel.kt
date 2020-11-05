package io.legado.app.base

import android.app.Application
import android.content.Context
import androidx.annotation.CallSuper
import androidx.lifecycle.AndroidViewModel
import io.legado.app.App
import io.legado.app.help.coroutine.Coroutine
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.toast
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
open class BaseViewModel(application: Application) : AndroidViewModel(application),
    CoroutineScope by MainScope(),
    AnkoLogger {

    val context: Context by lazy { this.getApplication<App>() }

    fun <T> execute(
        scope: CoroutineScope = this,
        context: CoroutineContext = Dispatchers.IO,
        block: suspend CoroutineScope.() -> T
    ): Coroutine<T> {
        return Coroutine.async(scope, context) { block() }
    }

    fun <R> submit(
        scope: CoroutineScope = this,
        context: CoroutineContext = Dispatchers.IO,
        block: suspend CoroutineScope.() -> Deferred<R>
    ): Coroutine<R> {
        return Coroutine.async(scope, context) { block().await() }
    }

    @CallSuper
    override fun onCleared() {
        super.onCleared()
        cancel()
    }

    open fun toast(message: Int) {
        launch {
            context.toast(message)
        }
    }

    open fun toast(message: CharSequence?) {
        launch {
            context.toast(message ?: toString())
        }
    }

    open fun longToast(message: Int) {
        launch {
            context.toast(message)
        }
    }

    open fun longToast(message: CharSequence?) {
        launch {
            context.toast(message ?: toString())
        }
    }
}