package io.legado.app.model

class ContentEmptyException(msg: String) : Exception(msg)

class ConcurrentException(msg: String, val waitTime: Long) : Exception(msg)