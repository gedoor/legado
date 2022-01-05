@file:Suppress("unused")

package io.legado.app.model

/**
 * 不记录错误堆栈的报错
 */
open class NoStackTraceException(msg: String) : Exception(msg) {

    override fun fillInStackTrace(): Throwable {
        return this
    }

}

/**
 * 目录为空
 */
class TocEmptyException(msg: String) : NoStackTraceException(msg)

/**
 * 内容为空
 */
class ContentEmptyException(msg: String) : NoStackTraceException(msg)

/**
 * 并发限制
 */
class ConcurrentException(msg: String, val waitTime: Int) : NoStackTraceException(msg)