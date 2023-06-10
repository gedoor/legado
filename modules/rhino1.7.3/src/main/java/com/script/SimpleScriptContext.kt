/*
 * Decompiled with CFR 0.152.
 */
package com.script

import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.Reader
import java.io.Writer
import java.util.*

open class SimpleScriptContext(
    private var engineScope: Bindings = SimpleBindings(),
    override var errorWriter: Writer = PrintWriter(System.err, true),
    override var reader: Reader = InputStreamReader(System.`in`),
    override var writer: Writer = PrintWriter(System.out, true)
) : ScriptContext {
    private var globalScope: Bindings? = null

    override fun setBindings(bindings: Bindings?, scope: Int) {
        when (scope) {
            100 -> {
                if (bindings == null) {
                    throw NullPointerException("Engine scope cannot be null.")
                }
                engineScope = bindings
                return
            }

            200 -> {
                globalScope = bindings
                return
            }
        }
        throw IllegalArgumentException("Invalid scope value.")
    }

    override fun getAttribute(name: String): Any? {
        if (engineScope.containsKey(name)) {
            return this.getAttribute(name, 100)
        }
        return if (globalScope?.containsKey(name) != true) {
            null
        } else this.getAttribute(name, 200)
    }

    override fun getAttribute(name: String, scope: Int): Any? {
        when (scope) {
            100 -> {
                return engineScope[name]
            }

            200 -> {
                return globalScope?.get(name)
            }
        }
        throw IllegalArgumentException("Illegal scope value.")
    }

    override fun removeAttribute(name: String, scope: Int): Any? {
        when (scope) {
            100 -> {
                return getBindings(100)?.remove(name)
            }

            200 -> {
                return getBindings(200)?.remove(name)
            }
        }
        throw IllegalArgumentException("Illegal scope value.")
    }

    override fun setAttribute(name: String, value: Any?, scope: Int) {
        when (scope) {
            100 -> {
                engineScope[name] = value
                return
            }

            200 -> {
                globalScope?.put(name, value)
                return
            }
        }
        throw IllegalArgumentException("Illegal scope value.")
    }

    override fun getAttributesScope(name: String): Int {
        if (engineScope.containsKey(name)) {
            return 100
        }
        return if (globalScope?.containsKey(name) != true) {
            -1
        } else 200
    }

    override fun getBindings(scope: Int): Bindings? {
        if (scope == 100) {
            return engineScope
        }
        if (scope == 200) {
            return globalScope
        }
        throw IllegalArgumentException("Illegal scope value.")
    }

    override val scopes: List<Int>
        get() = Companion.scopes

    companion object {
        private var scopes: MutableList<Int> = ArrayList(2)

        init {
            scopes.add(100)
            scopes.add(200)
            scopes = Collections.unmodifiableList(scopes)
        }
    }
}