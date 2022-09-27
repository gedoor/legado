@file:Suppress("unused")

package io.legado.app.utils

import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

/** This main looper cache avoids synchronization overhead when accessed repeatedly. */
private val mainLooper: Looper = Looper.getMainLooper()

private val mainThread: Thread = mainLooper.thread

private val isMainThread: Boolean inline get() = mainThread === Thread.currentThread()

fun buildMainHandler(): Handler {
    return if (SDK_INT >= 28) Handler.createAsync(mainLooper) else try {
        Handler::class.java.getDeclaredConstructor(
            Looper::class.java,
            Handler.Callback::class.java,
            Boolean::class.javaPrimitiveType // async
        ).newInstance(mainLooper, null, true)
    } catch (ignored: NoSuchMethodException) {
        // Hidden constructor absent. Fall back to non-async constructor.
        Handler(mainLooper)
    }
}

private val mainHandler by lazy { buildMainHandler() }

fun runOnUI(function: () -> Unit) {
    if (isMainThread) {
        function()
    } else {
        mainHandler.post(function)
    }
}

fun CoroutineScope.runOnIO(function: () -> Unit) {
    if (isMainThread) {
        launch(IO) {
            function()
        }
    } else {
        function()
    }
}