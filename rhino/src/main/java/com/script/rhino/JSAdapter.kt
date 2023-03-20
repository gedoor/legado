/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

import org.mozilla.javascript.*
import org.mozilla.javascript.Function

/**
 * JSAdapter is java.lang.reflect.Proxy equivalent for JavaScript. JSAdapter
 * calls specially named JavaScript methods on an adaptee object when property
 * access is attempted on it.
 *
 * Example:
 *
 * var y = {
 * __get__    : function (name) { ... }
 * __has__    : function (name) { ... }
 * __put__    : function (name, value) {...}
 * __delete__ : function (name) { ... }
 * __getIds__ : function () { ... }
 * };
 *
 * var x = new JSAdapter(y);
 *
 * x.i;                        // calls y.__get__
 * i in x;                     // calls y.__has__
 * x.p = 10;                   // calls y.__put__
 * delete x.p;                 // calls y.__delete__
 * for (i in x) { print(i); }  // calls y.__getIds__
 *
 * If a special JavaScript method is not found in the adaptee, then JSAdapter
 * forwards the property access to the adaptee itself.
 *
 * JavaScript caller of adapter object is isolated from the fact that
 * the property access/mutation/deletion are really calls to
 * JavaScript methods on adaptee.  Use cases include 'smart'
 * properties, property access tracing/debugging, encaptulation with
 * easy client access - in short JavaScript becomes more "Self" like.
 *
 * Note that Rhino already supports special properties like __proto__
 * (to set, get prototype), __parent__ (to set, get parent scope). We
 * follow the same double underscore nameing convention here. Similarly
 * the name JSAdapter is derived from JavaAdapter -- which is a facility
 * to extend, implement Java classes/interfaces by JavaScript.
 *
 * @author A. Sundararajan
 * @since 1.6
 */
@Suppress("unused", "UNUSED_PARAMETER")
class JSAdapter private constructor(var adaptee: Scriptable) : Scriptable, Function {
    private var prototype: Scriptable? = null
    private var parent: Scriptable? = null
    private var isPrototype = false

    override fun getClassName(): String {
        return "JSAdapter"
    }

    override fun get(name: String, start: Scriptable): Any {
        val func = getAdapteeFunction(GET_PROP)
        return if (func != null) {
            this.call(func, arrayOf(name))
        } else {
            adaptee[name, adaptee]
        }
    }

    override fun get(index: Int, start: Scriptable): Any {
        val func = getAdapteeFunction(GET_PROP)
        return if (func != null) {
            this.call(func, arrayOf(index))
        } else {
            adaptee[index, adaptee]
        }
    }

    override fun has(name: String, start: Scriptable): Boolean {
        val func = getAdapteeFunction(HAS_PROP)
        return if (func != null) {
            val res = this.call(func, arrayOf(name))
            Context.toBoolean(res)
        } else {
            adaptee.has(name, adaptee)
        }
    }

    override fun has(index: Int, start: Scriptable): Boolean {
        val func = getAdapteeFunction(HAS_PROP)
        return if (func != null) {
            val res = this.call(func, arrayOf(index))
            Context.toBoolean(res)
        } else {
            adaptee.has(index, adaptee)
        }
    }

    override fun put(name: String, start: Scriptable, value: Any) {
        if (start === this) {
            val func = getAdapteeFunction(PUT_PROP)
            if (func != null) {
                this.call(func, arrayOf(name, value))
            } else {
                adaptee.put(name, adaptee, value)
            }
        } else {
            start.put(name, start, value)
        }
    }

    override fun put(index: Int, start: Scriptable, value: Any) {
        if (start === this) {
            val func = getAdapteeFunction(PUT_PROP)
            if (func != null) {
                this.call(func, arrayOf(index, value))
            } else {
                adaptee.put(index, adaptee, value)
            }
        } else {
            start.put(index, start, value)
        }
    }

    override fun delete(name: String) {
        val func = getAdapteeFunction(DEL_PROP)
        if (func != null) {
            this.call(func, arrayOf(name))
        } else {
            adaptee.delete(name)
        }
    }

    override fun delete(index: Int) {
        val func = getAdapteeFunction(DEL_PROP)
        if (func != null) {
            this.call(func, arrayOf(index))
        } else {
            adaptee.delete(index)
        }
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

    override fun getIds(): Array<Any?> {
        val func = getAdapteeFunction(GET_PROPIDS)
        return if (func == null) {
            adaptee.ids
        } else {
            val val1 = this.call(func, arrayOfNulls(0))
            val res: Array<Any?>
            when (val1) {
                is NativeArray -> {
                    res = arrayOfNulls(val1.length.toInt())
                    for (index in res.indices) {
                        res[index] = mapToId(val1[index, val1])
                    }
                    res
                }
                !is NativeJavaArray -> {
                    Context.emptyArgs
                }
                else -> {
                    val tmp = val1.unwrap()
                    if (tmp.javaClass == Array<Any>::class.java) {
                        val array = tmp as Array<*>
                        res = arrayOfNulls(array.size)
                        for (index in array.indices) {
                            res[index] = mapToId(array[index])
                        }
                    } else {
                        res = Context.emptyArgs
                    }
                    res
                }
            }
        }
    }

    override fun hasInstance(scriptable: Scriptable): Boolean {
        return if (scriptable is JSAdapter) {
            true
        } else {
            var proto = scriptable.prototype
            while (proto != null) {
                if (proto == this) {
                    return true
                }
                proto = proto.prototype
            }
            false
        }
    }

    override fun getDefaultValue(hint: Class<*>?): Any {
        return adaptee.getDefaultValue(hint)
    }

    @Throws(RhinoException::class)
    override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<Any>): Any {
        return if (isPrototype) {
            construct(cx, scope, args)
        } else {
            val tmp = adaptee
            if (tmp is Function) {
                tmp.call(cx, scope, tmp, args)
            } else {
                throw Context.reportRuntimeError("TypeError: not a function")
            }
        }
    }

    @Throws(RhinoException::class)
    override fun construct(cx: Context, scope: Scriptable, args: Array<Any>): Scriptable {
        val tmp: Scriptable?
        return if (isPrototype) {
            tmp = ScriptableObject.getTopLevelScope(scope)
            if (args.isNotEmpty()) {
                JSAdapter(Context.toObject(args[0], tmp))
            } else {
                throw Context.reportRuntimeError("JSAdapter requires adaptee")
            }
        } else {
            tmp = adaptee
            if (tmp is Function) {
                tmp.construct(cx, scope, args)
            } else {
                throw Context.reportRuntimeError("TypeError: not a constructor")
            }
        }
    }

    private fun mapToId(tmp: Any?): Any {
        return if (tmp is Double) tmp.toInt() else Context.toString(tmp)
    }

    private fun getAdapteeFunction(name: String): Function? {
        val o = ScriptableObject.getProperty(adaptee, name)
        return o as? Function
    }

    private fun call(func: Function, args: Array<Any?>): Any {
        val cx = Context.getCurrentContext()
        val thisObj = adaptee
        val scope = func.parentScope
        return try {
            func.call(cx, scope, thisObj, args)
        } catch (re: RhinoException) {
            throw Context.reportRuntimeError(re.message)
        }
    }

    companion object {
        private const val GET_PROP = "__get__"
        private const val HAS_PROP = "__has__"
        private const val PUT_PROP = "__put__"
        private const val DEL_PROP = "__delete__"
        private const val GET_PROPIDS = "__getIds__"

        @Throws(RhinoException::class)
        fun init(cx: Context, scope: Scriptable, sealed: Boolean) {
            val obj = JSAdapter(cx.newObject(scope))
            obj.parentScope = scope
            obj.setPrototype(getFunctionPrototype(scope))
            obj.isPrototype = true
            ScriptableObject.defineProperty(scope, "JSAdapter", obj, 2)
        }

        private fun getFunctionPrototype(scope: Scriptable): Scriptable {
            return ScriptableObject.getFunctionPrototype(scope)
        }
    }
}