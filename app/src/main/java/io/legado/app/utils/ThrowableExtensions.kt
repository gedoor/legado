package io.legado.app.utils

import java.io.IOException

val Throwable.msg: String
    get() {
        val stackTrace = stackTraceToString()
        val lMsg = this.localizedMessage ?: "noErrorMsg"
        return when {
            stackTrace.isNotEmpty() -> stackTrace
            else -> lMsg
        }
    }

fun Throwable.rethrowAsIOException(): IOException {
    val newException = IOException(this.message)
    newException.initCause(this)
    throw newException
}