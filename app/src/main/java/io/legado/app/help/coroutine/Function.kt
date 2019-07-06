package io.legado.app.help.coroutine

interface Function<T, R> {

    fun apply(t: T?): R

}