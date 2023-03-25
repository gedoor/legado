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

import com.script.Invocable
import org.mozilla.javascript.*
import org.mozilla.javascript.Function

/**
 * This class implements Rhino-like JavaAdapter to help implement a Java
 * interface in JavaScript. We support this using Invocable.getInterface.
 * Using this JavaAdapter, script author could write:
 *
 *
 * var r = new java.lang.Runnable() {
 * run: function() { script... }
 * };
 *
 *
 * r.run();
 * new java.lang.Thread(r).start();
 *
 *
 * Note that Rhino's JavaAdapter support allows extending a Java class and/or
 * implementing one or more interfaces. This JavaAdapter implementation does
 * not support these.
 *
 * @author A. Sundararajan
 * @since 1.6
 */
@Suppress("UNUSED_PARAMETER")
internal class JavaAdapter private constructor(private val engine: Invocable) : ScriptableObject(),
    Function {
    override fun getClassName(): String {
        return "JavaAdapter"
    }

    @Throws(RhinoException::class)
    override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<Any>): Any {
        return construct(cx, scope, args)
    }

    @Throws(RhinoException::class)
    override fun construct(cx: Context, scope: Scriptable, args: Array<Any>): Scriptable {
        return if (args.size == 2) {
            var clazz: Class<*>? = null
            val obj = args[0]
            if (obj is Wrapper) {
                val o = obj.unwrap()
                if (o is Class<*> && o.isInterface) {
                    clazz = o
                }
            } else if (obj is Class<*> && obj.isInterface) {
                clazz = obj
            }
            if (clazz == null) {
                throw Context.reportRuntimeError("JavaAdapter: first arg should be interface Class")
            } else {
                val topLevel = getTopLevelScope(scope)
                Context.toObject(
                    engine.getInterface(args[1], clazz),
                    topLevel
                )
            }
        } else {
            throw Context.reportRuntimeError("JavaAdapter requires two arguments")
        }
    }

    companion object {
        @JvmStatic
        @Throws(RhinoException::class)
        fun init(cx: Context, scope: Scriptable, sealed: Boolean) {
            val topLevel = scope as RhinoTopLevel
            val engine: Invocable = topLevel.scriptEngine
            val obj = JavaAdapter(engine)
            obj.parentScope = scope
            obj.prototype = getFunctionPrototype(scope)
            putProperty(topLevel, "JavaAdapter", obj)
        }
    }
}