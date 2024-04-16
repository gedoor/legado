/*
 * Decompiled with CFR 0.152.
 */
package com.script

import org.mozilla.javascript.Scriptable
import java.io.Reader
import java.io.StringReader
import kotlin.coroutines.CoroutineContext

abstract class AbstractScriptEngine(val bindings: Bindings? = null) : ScriptEngine {

    override var context: ScriptContext = SimpleScriptContext()

    init {
        bindings?.let {
            context.setBindings(bindings, 100)
        }
    }

    override fun getBindings(scope: Int): Bindings? {
        if (scope == 200) {
            return context.getBindings(200)
        }
        if (scope == 100) {
            return context.getBindings(100)
        }
        throw IllegalArgumentException("Invalid scope value.")
    }

    override fun setBindings(bindings: Bindings?, scope: Int) {
        when (scope) {
            200 -> {
                context.setBindings(bindings, 200)
            }

            100 -> {
                context.setBindings(bindings, 100)
            }

            else -> {
                throw IllegalArgumentException("Invalid scope value.")
            }
        }
    }

    override fun put(key: String, value: Any?) {
        getBindings(100)?.put(key, value)
    }

    override fun get(key: String): Any? {
        return getBindings(100)?.get(key)
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
        val gs = getBindings(200)
        if (gs != null) {
            ctx.setBindings(gs, 200)
        }
        return ctx
    }
}
