package io.legado.app.help.coroutine

internal interface CoroutineContainer {

    fun add(coroutine: Coroutine<*>): Boolean

    fun addAll(vararg coroutines: Coroutine<*>): Boolean

    fun remove(coroutine: Coroutine<*>): Boolean

    fun delete(coroutine: Coroutine<*>): Boolean

    fun clear()

}
