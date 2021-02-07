package io.legado.app.base

import android.app.Application
import android.content.Context
import androidx.annotation.CallSuper
import androidx.lifecycle.AndroidViewModel
import io.legado.app.App
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
open class BaseViewModel(application: Application) : AndroidViewModel(application),
    CoroutineScope by MainScope() {

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

    open fun toastOnUi(message: Int) {
        context.toastOnUi(message)
    }

    open fun toastOnUi(message: CharSequence?) {
        context.toastOnUi(message ?: toString())
    }

}