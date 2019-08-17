package io.legado.app.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.legado.app.help.coroutine.Coroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import org.jetbrains.anko.AnkoLogger

open class BaseViewModel(application: Application) : AndroidViewModel(application), CoroutineScope by MainScope(),
    AnkoLogger {

    fun <T> execute(scope: CoroutineScope = this, block: suspend CoroutineScope.() -> T): Coroutine<T> {
        return Coroutine.async(scope) { block() }
    }

    fun <R> submit(scope: CoroutineScope = this, block: suspend CoroutineScope.() -> Deferred<R>): Coroutine<R> {
        return Coroutine.async(scope) { block().await() }
    }

    override fun onCleared() {
        super.onCleared()
        cancel()
    }

    open fun toast(message: Int) {

    }
}