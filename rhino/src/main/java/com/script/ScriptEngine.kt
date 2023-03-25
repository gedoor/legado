/*
 * Decompiled with CFR 0.152.
 */
package com.script

import java.io.Reader

interface ScriptEngine {
    var context: ScriptContext

    fun createBindings(): Bindings?

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

    operator fun get(key: String): Any?

    fun getBindings(scope: Int): Bindings?

    fun put(key: String, value: Any?)

    fun setBindings(bindings: Bindings?, scope: Int)

    companion object {
        const val FILENAME = "javax.script.filename"
    }
}