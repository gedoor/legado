package io.legado.app.exception

/**
 * 不记录错误堆栈的报错
 */
open class NoStackTraceException(msg: String) : Exception(msg) {

    override fun fillInStackTrace(): Throwable {
        stackTrace = emptyStackTrace
        return this
    }

    companion object {
        private val emptyStackTrace = emptyArray<StackTraceElement>()
    }

}