package io.legado.app.utils

import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper

/** This main looper cache avoids synchronization overhead when accessed repeatedly. */
@JvmField
val mainLooper: Looper = Looper.getMainLooper()!!

@JvmField
val mainThread: Thread = mainLooper.thread

val isMainThread: Boolean inline get() = mainThread === Thread.currentThread()

@PublishedApi
internal val currentThread: Any?
    inline get() = Thread.currentThread()

@JvmField
val mainHandler: Handler = if (SDK_INT >= 28) Handler.createAsync(mainLooper) else try {
    Handler::class.java.getDeclaredConstructor(
        Looper::class.java,
        Handler.Callback::class.java,
        Boolean::class.javaPrimitiveType // async
    ).newInstance(mainLooper, null, true)
} catch (ignored: NoSuchMethodException) {
    Handler(mainLooper) // Hidden constructor absent. Fall back to non-async constructor.
}

fun runOnUiThread(f: () -> Unit) {
    if (isMainThread) {
        f()
    } else {
        mainHandler.post(f)
    }
}