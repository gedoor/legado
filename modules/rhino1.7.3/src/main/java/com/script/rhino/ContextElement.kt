package com.script.rhino

import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.CoroutineContext

class ContextElement : ThreadContextElement<Any?> {

    companion object Key : CoroutineContext.Key<ContextElement>

    override val key: CoroutineContext.Key<ContextElement>
        get() = Key

    private val contextHelper: Any? = VMBridgeReflect.contextLocal.get()

    override fun updateThreadContext(context: CoroutineContext): Any? {
        val oldState = VMBridgeReflect.contextLocal.get()
        VMBridgeReflect.contextLocal.set(contextHelper)
        return oldState
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: Any?) {
        VMBridgeReflect.contextLocal.set(oldState)
    }

}
