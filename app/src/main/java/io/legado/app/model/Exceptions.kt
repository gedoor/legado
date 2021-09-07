package io.legado.app.model

/**
 * 内容为空
 */
class ContentEmptyException(msg: String) : Exception(msg)

/**
 * 并发限制
 */
class ConcurrentException(msg: String, val waitTime: Long) : Exception(msg)