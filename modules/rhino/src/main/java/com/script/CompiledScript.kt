/*
 * Decompiled with CFR 0.152.
 */
package com.script

import com.script.ScriptContext.Companion.ENGINE_SCOPE
import com.script.ScriptContext.Companion.GLOBAL_SCOPE
import org.mozilla.javascript.Scriptable
import kotlin.coroutines.CoroutineContext

abstract class CompiledScript {

    abstract fun getEngine(): ScriptEngine

    @Throws(ScriptException::class)
    abstract fun eval(context: ScriptContext): Any?

    @Throws(ScriptException::class)
    abstract fun eval(scope: Scriptable): Any?

    @Throws(ScriptException::class)
    abstract fun eval(scope: Scriptable, coroutineContext: CoroutineContext?): Any?

    @Throws(ScriptException::class)
    abstract suspend fun evalSuspend(scope: Scriptable): Any?

    @Throws(ScriptException::class)
    fun eval(bindings: Bindings?): Any? {
        var ctxt = getEngine().context
        if (bindings != null) {
            val tempContext = SimpleScriptContext()
            tempContext.setBindings(bindings, ENGINE_SCOPE)
            tempContext.setBindings(ctxt.getBindings(GLOBAL_SCOPE), GLOBAL_SCOPE)
            tempContext.writer = ctxt.writer
            tempContext.reader = ctxt.reader
            tempContext.errorWriter = ctxt.errorWriter
            ctxt = tempContext
        }
        return this.eval(ctxt)
    }

    @Throws(ScriptException::class)
    fun eval(): Any? {
        return this.eval(getEngine().context)
    }
}