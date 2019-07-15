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

    fun <T> execute(block: suspend CoroutineScope.() -> T): Coroutine<T> {
        return Coroutine.launch(this) { block() }
    }

    fun <T> submit(block: suspend CoroutineScope.() -> Deferred<T>): Coroutine<T> {
        return Coroutine.launch(this) { block().await() }
    }


    override fun onCleared() {
        super.onCleared()
        cancel()
    }
}