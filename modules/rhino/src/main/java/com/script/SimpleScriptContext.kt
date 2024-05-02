/*
 * Decompiled with CFR 0.152.
 */
package com.script

import com.script.ScriptContext.Companion.ENGINE_SCOPE
import com.script.ScriptContext.Companion.GLOBAL_SCOPE
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.Reader
import java.io.Writer

open class SimpleScriptContext(
    private var engineScope: Bindings = SimpleBindings(),
    override var errorWriter: Writer = PrintWriter(System.err, true),
    override var reader: Reader = InputStreamReader(System.`in`),
    override var writer: Writer = PrintWriter(System.out, true)
) : ScriptContext {
    private var globalScope: Bindings? = null

    override fun setBindings(bindings: Bindings?, scope: Int) {
        when (scope) {
            ENGINE_SCOPE -> {
                if (bindings == null) {
                    throw NullPointerException("Engine scope cannot be null.")
                }
                engineScope = bindings
                return
            }

            GLOBAL_SCOPE -> {
                globalScope = bindings
                return
            }
        }
        throw IllegalArgumentException("Invalid scope value.")
    }

    override fun getAttribute(name: String): Any? {
        return if (engineScope.containsKey(name)) {
            this.getAttribute(name, ENGINE_SCOPE)
        } else if (globalScope?.containsKey(name) == true) {
            this.getAttribute(name, GLOBAL_SCOPE)
        } else {
            null
        }
    }

    override fun getAttribute(name: String, scope: Int): Any? {
        when (scope) {
            ENGINE_SCOPE -> {
                return engineScope[name]
            }

            GLOBAL_SCOPE -> {
                return globalScope?.get(name)
            }
        }
        throw IllegalArgumentException("Illegal scope value.")
    }

    override fun removeAttribute(name: String, scope: Int): Any? {
        when (scope) {
            ENGINE_SCOPE -> {
                return getBindings(ENGINE_SCOPE)?.remove(name)
            }

            GLOBAL_SCOPE -> {
                return getBindings(GLOBAL_SCOPE)?.remove(name)
            }
        }
        throw IllegalArgumentException("Illegal scope value.")
    }

    override fun setAttribute(name: String, value: Any?, scope: Int) {
        when (scope) {
            ENGINE_SCOPE -> engineScope[name] = value
            GLOBAL_SCOPE -> globalScope?.put(name, value)
            else -> throw IllegalArgumentException("Illegal scope value.")
        }
    }

    override fun getAttributesScope(name: String): Int {
        return if (engineScope.containsKey(name)) {
            ENGINE_SCOPE
        } else if (globalScope?.containsKey(name) == true) {
            GLOBAL_SCOPE
        } else {
            -1
        }
    }

    override fun getBindings(scope: Int): Bindings? {
        if (scope == ENGINE_SCOPE) {
            return engineScope
        }
        if (scope == GLOBAL_SCOPE) {
            return globalScope
        }
        throw IllegalArgumentException("Illegal scope value.")
    }

    override val scopes: List<Int>
        get() = Companion.scopes

    companion object {
        private val scopes = listOf(ENGINE_SCOPE, GLOBAL_SCOPE)
    }
}