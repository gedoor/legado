/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.script.rhino

import com.script.ScriptContext
import org.mozilla.javascript.*
import org.mozilla.javascript.Function

/**
 * ExternalScriptable is an implementation of Scriptable
 * backed by a JSR 223 ScriptContext instance.
 *
 * @author Mike Grogan
 * @author A. Sundararajan
 * @since 1.6
 */
internal class ExternalScriptable @JvmOverloads constructor(
    val context: ScriptContext,
    private val indexedProps: MutableMap<Any, Any?> = HashMap()
) : Scriptable {
    private var prototype: Scriptable? = null
    private var parent: Scriptable? = null

    private fun isEmpty(name: String): Boolean {
        return name == ""
    }

    override fun getClassName(): String {
        return "Global"
    }

    @Synchronized
    override fun get(name: String, start: Scriptable): Any? {
        return if (this.isEmpty(name)) {
            indexedProps.getOrElse(name) { Scriptable.NOT_FOUND }
        } else {
            synchronized(context) {
                val scope = context.getAttributesScope(name)
                return if (scope != -1) {
                    val value = context.getAttribute(name, scope)
                    Context.javaToJS(value, this)
                } else {
                    Scriptable.NOT_FOUND
                }
            }
        }
    }

    @Synchronized
    override fun get(index: Int, start: Scriptable): Any? {
        return indexedProps.getOrElse(index) { Scriptable.NOT_FOUND }
    }

    @Synchronized
    override fun has(name: String, start: Scriptable): Boolean {
        return if (this.isEmpty(name)) {
            indexedProps.containsKey(name)
        } else {
            synchronized(context) { return context.getAttributesScope(name) != -1 }
        }
    }

    @Synchronized
    override fun has(index: Int, start: Scriptable): Boolean {
        return indexedProps.containsKey(index)
    }

    override fun put(name: String, start: Scriptable, value: Any?) {
        if (start === this) {
            synchronized(this) {
                if (this.isEmpty(name)) {
                    indexedProps.put(name, value)
                } else {
                    synchronized(context) {
                        var scope = context.getAttributesScope(name)
                        if (scope == -1) {
                            scope = 100
                        }
                        context.setAttribute(name, jsToJava(value), scope)
                    }
                }
            }
        } else {
            start.put(name, start, value)
        }
    }

    override fun put(index: Int, start: Scriptable, value: Any?) {
        if (start === this) {
            synchronized(this) { indexedProps.put(index, value) }
        } else {
            start.put(index, start, value)
        }
    }

    @Synchronized
    override fun delete(name: String) {
        if (this.isEmpty(name)) {
            indexedProps.remove(name)
        } else {
            synchronized(context) {
                val scope = context.getAttributesScope(name)
                if (scope != -1) {
                    context.removeAttribute(name, scope)
                }
            }
        }
    }

    override fun delete(index: Int) {
        indexedProps.remove(index)
    }

    override fun getPrototype(): Scriptable? {
        return prototype
    }

    override fun setPrototype(prototype: Scriptable?) {
        this.prototype = prototype
    }

    override fun getParentScope(): Scriptable? {
        return parent
    }

    override fun setParentScope(parent: Scriptable?) {
        this.parent = parent
    }

    @Synchronized
    override fun getIds(): Array<Any> {
        val keys = allKeys
        val size = keys.size + indexedProps.size
        val res = arrayOfNulls<Any>(size)
        System.arraycopy(keys, 0, res, 0, keys.size)
        var i = keys.size
        var index: Any
        val var5: Iterator<*> = indexedProps.keys.iterator()
        while (var5.hasNext()) {
            index = var5.next()!!
            res[i++] = index
        }
        @Suppress("UNCHECKED_CAST")
        return res as Array<Any>
    }

    override fun getDefaultValue(typeHint: Class<*>?): Any {
        for (i in 0..1) {
            val tryToString: Boolean =
                if (typeHint == ScriptRuntime.StringClass) {
                    i == 0
                } else {
                    i == 1
                }
            var methodName: String
            var args: Array<Any?>
            if (tryToString) {
                methodName = "toString"
                args = ScriptRuntime.emptyArgs
            } else {
                methodName = "valueOf"
                args = arrayOfNulls(1)
                val hint: String = when {
                    typeHint == null -> {
                        "undefined"
                    }
                    typeHint == ScriptRuntime.StringClass -> {
                        "string"
                    }
                    typeHint == ScriptRuntime.ScriptableClass -> {
                        "object"
                    }
                    typeHint == ScriptRuntime.FunctionClass -> {
                        "function"
                    }
                    typeHint != ScriptRuntime.BooleanClass && typeHint != java.lang.Boolean.TYPE -> {
                        if (typeHint != ScriptRuntime.NumberClass
                            && typeHint != ScriptRuntime.ByteClass
                            && typeHint != java.lang.Byte.TYPE
                            && typeHint != ScriptRuntime.ShortClass
                            && typeHint != java.lang.Short.TYPE
                            && typeHint != ScriptRuntime.IntegerClass
                            && typeHint != Integer.TYPE
                            && typeHint != ScriptRuntime.FloatClass
                            && typeHint != java.lang.Float.TYPE
                            && typeHint != ScriptRuntime.DoubleClass
                            && typeHint != java.lang.Double.TYPE
                        ) {
                            throw Context.reportRuntimeError("Invalid JavaScript value of type $typeHint")
                        }
                        "number"
                    }
                    else -> {
                        "boolean"
                    }
                }
                args[0] = hint
            }
            var v = ScriptableObject.getProperty(this, methodName)
            if (v is Function) {
                val function = v
                val cx = Context.enter()
                v = try {
                    function.call(cx, function.parentScope, this, args)
                } finally {
                    Context.exit()
                }
                if (v != null) {
                    if (v !is Scriptable) {
                        return v
                    }
                    if (typeHint == ScriptRuntime.ScriptableClass || typeHint == ScriptRuntime.FunctionClass) {
                        return v
                    }
                    if (tryToString && v is Wrapper) {
                        val u = (v as Wrapper).unwrap()
                        if (u is String) {
                            return u
                        }
                    }
                }
            }
        }
        val arg = if (typeHint == null) "undefined" else typeHint.name
        throw Context.reportRuntimeError("找不到对象的默认值 $arg")
    }

    override fun hasInstance(instance: Scriptable): Boolean {
        var proto = instance.prototype
        while (proto != null) {
            if (proto == this) {
                return true
            }
            proto = proto.prototype
        }
        return false
    }

    private val allKeys: Array<String>
        get() {
            val list = ArrayList<String>()
            synchronized(context) {
                val iterator: Iterator<*> = context.scopes.iterator()
                while (iterator.hasNext()) {
                    val scope = iterator.next() as Int
                    val bindings = context.getBindings(scope)
                    if (bindings != null) {
                        list.ensureCapacity(bindings.size)
                        val iterator1: Iterator<*> = bindings.keys.iterator()
                        while (iterator1.hasNext()) {
                            val key = iterator1.next() as String
                            list.add(key)
                        }
                    }
                }
            }
            return list.toTypedArray()
        }

    private fun jsToJava(jsObj: Any?): Any? {
        return if (jsObj is Wrapper) {
            if (jsObj is NativeJavaClass) {
                jsObj
            } else {
                val obj = jsObj.unwrap()
                if (obj !is Number && obj !is String && obj !is Boolean && obj !is Char) obj else jsObj
            }
        } else {
            jsObj
        }
    }
}