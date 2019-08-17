package io.legado.app.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.legado.app.App
import io.legado.app.help.coroutine.Coroutine
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.toast

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
        launch {
            getApplication<App>().toast(message)
        }
    }

    open fun toast(message: CharSequence) {
        launch {
            getApplication<App>().toast(message)
        }
    }

    open fun longToast(message: Int) {
        launch {
            getApplication<App>().toast(message)
        }
    }

    open fun longToast(message: CharSequence) {
        launch {
            getApplication<App>().toast(message)
        }
    }
}