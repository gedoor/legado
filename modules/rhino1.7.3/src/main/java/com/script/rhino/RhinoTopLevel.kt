/*
 * Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
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

import com.script.Bindings
import com.script.ScriptContext
import com.script.SimpleScriptContext
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.ImporterTopLevel
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Synchronizer
import org.mozilla.javascript.Wrapper
import java.security.AccessControlContext

/**
 * This class serves as top level scope for Rhino. This class adds
 * 3 top level functions (bindings, scope, sync) and two constructors
 * (JSAdapter, JavaAdapter).
 *
 * @author A. Sundararajan
 * @since 1.6
 */
@Suppress("UNUSED_PARAMETER", "unused")
class RhinoTopLevel(cx: Context, val scriptEngine: RhinoScriptEngine) :
    ImporterTopLevel(cx, System.getSecurityManager() != null) {

    init {
//        LazilyLoadedCtor(this, "JSAdapter", "com.script.rhino.JSAdapter", false)
//        JavaAdapter.init(cx, this, false)
//        val names = arrayOf("bindings", "scope", "sync")
//        defineFunctionProperties(names, RhinoTopLevel::class.java, 2)
    }

    val accessContext: AccessControlContext?
        get() = scriptEngine.accessContext

    companion object {

        @JvmStatic
        fun bindings(
            cx: Context,
            thisObj: Scriptable?,
            args: Array<Any?>,
            funObj: Function?
        ): Any {
            if (args.size == 1) {
                var arg = args[0]
                if (arg is Wrapper) {
                    arg = arg.unwrap()
                }
                if (arg is ExternalScriptable) {
                    val ctx = arg.context
                    val bind = ctx.getBindings(100)
                    return Context.javaToJS(bind, getTopLevelScope(thisObj))
                }
            }
            return Context.getUndefinedValue()
        }

        @JvmStatic
        fun scope(cx: Context, thisObj: Scriptable?, args: Array<Any?>, funObj: Function?): Any {
            if (args.size == 1) {
                var arg = args[0]
                if (arg is Wrapper) {
                    arg = arg.unwrap()
                }
                if (arg is Bindings) {
                    val ctx: ScriptContext = SimpleScriptContext()
                    ctx.setBindings(arg as Bindings?, 100)
                    val res: Scriptable = ExternalScriptable(ctx)
                    res.prototype = getObjectPrototype(thisObj)
                    res.parentScope = getTopLevelScope(thisObj)
                    return res
                }
            }
            return Context.getUndefinedValue()
        }

        @JvmStatic
        fun sync(cx: Context, thisObj: Scriptable?, args: Array<Any?>, funObj: Function?): Any {
            return if (args.size == 1 && args[0] is Function) {
                Synchronizer(args[0] as Function?)
            } else {
                throw Context.reportRuntimeError("wrong argument(s) for sync")
            }
        }
    }
}