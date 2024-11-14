/*
 * Decompiled with CFR 0.152.
 */
package com.script

import com.script.ScriptContext.Companion.ENGINE_SCOPE
import com.script.ScriptContext.Companion.GLOBAL_SCOPE
import org.mozilla.javascript.Scriptable
import java.io.Reader
import java.io.StringReader
import kotlin.coroutines.CoroutineContext

abstract class AbstractScriptEngine(val bindings: Bindings? = null) : ScriptEngine {

    override var context: ScriptContext = SimpleScriptContext()

    init {
        bindings?.let {
            context.setBindings(bindings, ENGINE_SCOPE)
        }
    }

    override fun getBindings(scope: Int): Bindings? {
        if (scope == GLOBAL_SCOPE) {
            return context.getBindings(GLOBAL_SCOPE)
        }
        if (scope == ENGINE_SCOPE) {
            return context.getBindings(ENGINE_SCOPE)
        }
        throw IllegalArgumentException("Invalid scope value.")
    }

    override fun setBindings(bindings: Bindings?, scope: Int) {
        when (scope) {
            GLOBAL_SCOPE -> {
                context.setBindings(bindings, GLOBAL_SCOPE)
            }

            ENGINE_SCOPE -> {
                context.setBindings(bindings, ENGINE_SCOPE)
            }

            else -> {
                throw IllegalArgumentException("Invalid scope value.")
            }
        }
    }

    override fun put(key: String, value: Any?) {
        getBindings(ENGINE_SCOPE)?.put(key, value)
    }

    override fun get(key: String): Any? {
        return getBindings(ENGINE_SCOPE)?.get(key)
    }

    override suspend fun evalSuspend(script: String, scope: Scriptable): Any? {
        return this.evalSuspend(StringReader(script), scope)
    }

    override fun eval(script: String, scope: Scriptable): Any? {
        return this.eval(StringReader(script), scope)
    }

    @Throws(ScriptException::class)
    override fun eval(reader: Reader, context: ScriptContext): Any? {
        return this.eval(reader, getRuntimeScope(context))
    }

    override fun eval(script: String, scope: Scriptable, coroutineContext: CoroutineContext?): Any? {
        return this.eval(StringReader(script), scope, coroutineContext)
    }

    @Throws(ScriptException::class)
    override fun eval(reader: Reader, bindings: Bindings): Any? {
        return this.eval(reader, getScriptContext(bindings))
    }

    @Throws(ScriptException::class)
    override fun eval(script: String, bindings: Bindings): Any? {
        return this.eval(script, getScriptContext(bindings))
    }

    @Throws(ScriptException::class)
    override fun eval(script: String, bindings: ScriptBindings): Any? {
        return this.eval(script, getRuntimeScope(bindings))
    }

    @Throws(ScriptException::class)
    override fun eval(reader: Reader): Any? {
        return this.eval(reader, context)
    }

    @Throws(ScriptException::class)
    override fun eval(script: String): Any? {
        return this.eval(script, context)
    }

    @Throws(ScriptException::class)
    override fun eval(script: String, context: ScriptContext): Any? {
        return this.eval(StringReader(script), context)
    }

    override fun getScriptContext(bindings: Bindings): ScriptContext {
        val ctx = SimpleScriptContext(bindings, context.errorWriter, context.reader, context.writer)
        val gs = getBindings(GLOBAL_SCOPE)
        if (gs != null) {
            ctx.setBindings(gs, GLOBAL_SCOPE)
        }
        return ctx
    }
}
