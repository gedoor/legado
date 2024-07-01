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

import com.script.CompiledScript
import com.script.ScriptContext
import com.script.ScriptEngine
import com.script.ScriptException
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import org.mozilla.javascript.*
import java.io.IOException
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/**
 * Represents compiled JavaScript code.
 *
 * @author Mike Grogan
 * @since 1.6
 */
internal class RhinoCompiledScript(
    private val engine: RhinoScriptEngine,
    private val script: Script
) : CompiledScript() {

    override fun getEngine(): ScriptEngine {
        return engine
    }

    @Throws(ScriptException::class)
    override fun eval(context: ScriptContext): Any? {
        val cx = Context.enter()
        val result: Any?
        try {
            val scope = engine.getRuntimeScope(context)
            val ret = script.exec(cx, scope)
            result = engine.unwrapReturnValue(ret)
        } catch (re: RhinoException) {
            val line = if (re.lineNumber() == 0) -1 else re.lineNumber()
            val msg: String = if (re is JavaScriptException) {
                re.value.toString()
            } else {
                re.toString()
            }
            val se = ScriptException(msg, re.sourceName(), line)
            se.initCause(re)
            throw se
        } finally {
            Context.exit()
        }
        return result
    }

    override fun eval(scope: Scriptable): Any? {
        val cx = Context.enter()
        val result: Any?
        try {
            val ret = script.exec(cx, scope)
            result = engine.unwrapReturnValue(ret)
        } catch (re: RhinoException) {
            val line = if (re.lineNumber() == 0) -1 else re.lineNumber()
            val msg: String = if (re is JavaScriptException) {
                re.value.toString()
            } else {
                re.toString()
            }
            val se = ScriptException(msg, re.sourceName(), line)
            se.initCause(re)
            throw se
        } finally {
            Context.exit()
        }
        return result
    }

    override fun eval(scope: Scriptable, coroutineContext: CoroutineContext?): Any? {
        val cx = Context.enter()
        if (cx is RhinoContext) {
            cx.coroutineContext = coroutineContext
        }
        val result: Any?
        try {
            val ret = script.exec(cx, scope)
            result = engine.unwrapReturnValue(ret)
        } catch (re: RhinoException) {
            val line = if (re.lineNumber() == 0) -1 else re.lineNumber()
            val msg: String = if (re is JavaScriptException) {
                re.value.toString()
            } else {
                re.toString()
            }
            val se = ScriptException(msg, re.sourceName(), line)
            se.initCause(re)
            throw se
        } finally {
            Context.exit()
        }
        return result
    }

    override suspend fun evalSuspend(scope: Scriptable): Any? {
        val cx = Context.enter()
        var ret: Any?
        withContext(VMBridgeReflect.contextLocal.asContextElement()) {
            try {
                try {
                    ret = cx.executeScriptWithContinuations(script, scope)
                } catch (e: ContinuationPending) {
                    var pending = e
                    while (true) {
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val suspendFunction =
                                pending.applicationState as Function1<Continuation<Any?>, Any?>
                            val functionResult = suspendCoroutineUninterceptedOrReturn { cout ->
                                suspendFunction.invoke(cout)
                            }
                            val continuation = pending.continuation
                            ret = cx.resumeContinuation(continuation, scope, functionResult)
                            break
                        } catch (e: ContinuationPending) {
                            pending = e
                        }
                    }
                }
            } catch (re: RhinoException) {
                val line = if (re.lineNumber() == 0) -1 else re.lineNumber()
                val msg: String = if (re is JavaScriptException) {
                    re.value.toString()
                } else {
                    re.toString()
                }
                val se = ScriptException(msg, re.sourceName(), line)
                se.initCause(re)
                throw se
            } catch (var14: IOException) {
                throw ScriptException(var14)
            } finally {
                Context.exit()
            }
        }
        return engine.unwrapReturnValue(ret)
    }

}