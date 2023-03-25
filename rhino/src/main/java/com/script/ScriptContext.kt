/*
 * Decompiled with CFR 0.152.
 */
package com.script

import java.io.Reader
import java.io.Writer

interface ScriptContext {

    var errorWriter: Writer

    var reader: Reader

    val scopes: List<Int>

    var writer: Writer

    fun getAttribute(name: String): Any?

    fun getAttribute(name: String, scope: Int): Any?

    fun getAttributesScope(name: String): Int

    fun getBindings(scope: Int): Bindings?

    fun removeAttribute(name: String, scope: Int): Any?

    fun setAttribute(name: String, value: Any?, scope: Int)

    fun setBindings(bindings: Bindings?, scope: Int)

    companion object {
        const val ENGINE_SCOPE = 100
        const val GLOBAL_SCOPE = 200
    }
}