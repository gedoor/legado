/*
 * Decompiled with CFR 0.152.
 */
package com.script

import org.mozilla.javascript.Scriptable
import java.io.Reader
import kotlin.coroutines.CoroutineContext

interface ScriptEngine {
    var context: ScriptContext

    fun createBindings(): Bindings?

    @Throws(ScriptException::class)
    fun eval(reader: Reader, scope: Scriptable): Any?

    @Throws(ScriptException::class)
    fun eval(reader: Reader, scope: Scriptable, coroutineContext: CoroutineContext?): Any?

    @Throws(ScriptException::class)
    suspend fun evalSuspend(reader: Reader, scope: Scriptable): Any?

    @Throws(ScriptException::class)
    fun eval(script: String, scope: Scriptable): Any?

    @Throws(ScriptException::class)
    suspend fun evalSuspend(script: String, scope: Scriptable): Any?

    @Throws(ScriptException::class)
    fun eval(script: String, scope: Scriptable, coroutineContext: CoroutineContext?): Any?

    @Throws(ScriptException::class)
    fun eval(reader: Reader): Any?

    @Throws(ScriptException::class)
    fun eval(reader: Reader, bindings: Bindings): Any?

    @Throws(ScriptException::class)
    fun eval(reader: Reader, context: ScriptContext): Any?

    @Throws(ScriptException::class)
    fun eval(script: String): Any?

    @Throws(ScriptException::class)
    fun eval(script: String, bindings: Bindings): Any?

    @Throws(ScriptException::class)
    fun eval(script: String, context: ScriptContext): Any?

    fun getRuntimeScope(context: ScriptContext): Scriptable

    fun getScriptContext(bindings: Bindings): ScriptContext

    operator fun get(key: String): Any?

    fun getBindings(scope: Int): Bindings?

    fun put(key: String, value: Any?)

    fun setBindings(bindings: Bindings?, scope: Int)

    companion object {
        const val FILENAME = "javax.script.filename"
    }
}