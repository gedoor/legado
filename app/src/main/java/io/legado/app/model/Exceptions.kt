@file:Suppress("unused")

package io.legado.app.model

class AppException(msg: String) : Exception(msg)

/**
 *
 */
class NoStackTraceException(msg: String) : Exception(msg) {

    override fun fillInStackTrace(): Throwable {
        return this
    }

}

/**
 * 目录为空
 */
class TocEmptyException(msg: String) : Exception(msg) {

    override fun fillInStackTrace(): Throwable {
        return this
    }

}

/**
 * 内容为空
 */
class ContentEmptyException(msg: String) : Exception(msg) {

    override fun fillInStackTrace(): Throwable {
        return this
    }

}

/**
 * 并发限制
 */
class ConcurrentException(msg: String, val waitTime: Int) : Exception(msg) {

    override fun fillInStackTrace(): Throwable {
        return this
    }

}