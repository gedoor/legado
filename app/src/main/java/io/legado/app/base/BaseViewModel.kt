package io.legado.app.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.legado.app.help.coroutine.Coroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import org.jetbrains.anko.AnkoLogger

open class BaseViewModel(application: Application) : AndroidViewModel(application), CoroutineScope by MainScope(),
    AnkoLogger {

    fun <T> execute(domain: suspend CoroutineScope.() -> T): Coroutine<T> {
        return Coroutine.with(this) { domain() }
    }

    override fun onCleared() {
        super.onCleared()
        cancel()
    }
}