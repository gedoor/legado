/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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
import com.script.ScriptException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.security.AccessControlContext
import java.security.AccessController
import java.security.PrivilegedExceptionAction

/**
 * java.lang.reflect.Proxy based interface implementor. This is meant
 * to be used to implement Invocable.getInterface.
 *
 * @author Mike Grogan
 * @since 1.6
 */
@Suppress("UNUSED_PARAMETER")
open class InterfaceImplementor(private val engine: Invocable) {
    @Throws(ScriptException::class)
    fun <T> getInterface(obj: Any?, clazz: Class<T>?): T? {
        return if (clazz != null && clazz.isInterface) {
            if (!isImplemented(obj, clazz)) {
                null
            } else {
                val accContext = AccessController.getContext()
                clazz.cast(
                    Proxy.newProxyInstance(
                        clazz.classLoader,
                        arrayOf<Class<*>>(clazz),
                        InterfaceImplementorInvocationHandler(obj, accContext)
                    )
                )
            }
        } else {
            throw IllegalArgumentException("interface Class expected")
        }
    }

    protected open fun isImplemented(obj: Any?, clazz: Class<*>): Boolean {
        return true
    }

    @Throws(ScriptException::class)
    protected open fun convertResult(method: Method?, res: Any?): Any? {
        return res
    }

    @Throws(ScriptException::class)
    protected fun convertArguments(method: Method?, args: Array<Any>): Array<Any> {
        return args
    }

    private inner class InterfaceImplementorInvocationHandler(
        private val obj: Any?,
        private val accContext: AccessControlContext
    ) : InvocationHandler {

        @Throws(Throwable::class)
        override fun invoke(proxy: Any, method: Method, args: Array<Any>): Any? {
            val finalArgs = convertArguments(method, args)
            val result = AccessController.doPrivileged(PrivilegedExceptionAction {
                if (obj == null) engine.invokeFunction(method.name, *finalArgs)
                else engine.invokeMethod(obj, method.name, *finalArgs)
            }, accContext)
            return convertResult(method, result)
        }

    }
}