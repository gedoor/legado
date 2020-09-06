package io.legado.app.utils

val Throwable.msg: String
    get() {
        val lMsg = this.localizedMessage
        return if (lMsg.isNullOrEmpty()) this.stackTraceToString() else {
            lMsg
        }
    }