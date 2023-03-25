/*
 * Decompiled with CFR 0.152.
 */
package com.script

import java.io.Reader

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
        val nn = getBindings(100)
        if (nn != null) {
            nn[key] = value
        }
    }

    override fun get(key: String): Any? {
        val nn = getBindings(100)
        return nn?.get(key)
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

    protected fun getScriptContext(nn: Bindings): ScriptContext {
        val ctx = SimpleScriptContext()
        val gs = getBindings(200)
        if (gs != null) {
            ctx.setBindings(gs, 200)
        }
        ctx.setBindings(nn, 100)
        ctx.reader = context.reader
        ctx.writer = context.writer
        ctx.errorWriter = context.errorWriter
        return ctx
    }
}