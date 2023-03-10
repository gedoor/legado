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

package com.script.rhino;

import com.script.Invocable;
import com.script.ScriptException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;


/*
 * java.lang.reflect.Proxy based interface implementor. This is meant
 * to be used to implement Invocable.getInterface.
 *
 * @author Mike Grogan
 * @since 1.6
 */
public class InterfaceImplementor {

    private Invocable engine;

    /** Creates a new instance of Invocable */
    public InterfaceImplementor(Invocable engine) {
        this.engine = engine;
    }

    private final class InterfaceImplementorInvocationHandler
            implements InvocationHandler {
        private Object thiz;
        private AccessControlContext accCtxt;

        public InterfaceImplementorInvocationHandler(Object thiz,
                                                     AccessControlContext accCtxt) {
            this.thiz = thiz;
            this.accCtxt = accCtxt;
        }

        public Object invoke(Object proxy , Method method, Object[] args)
                throws java.lang.Throwable {
            // give chance to convert input args
            args = convertArguments(method, args);
            Object result;
            final Method m = method;
            final Object[] a = args;
            result = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    if (thiz == null) {
                        return engine.invokeFunction(m.getName(), a);
                    } else {
                        return engine.invokeMethod(thiz, m.getName(), a);
                    }
                }
            }, accCtxt);
            // give chance to convert the method result
            return convertResult(method, result);
        }
    }

    public <T> T getInterface(Object thiz, Class<T> iface)
            throws ScriptException {
        if (iface == null || !iface.isInterface()) {
            throw new IllegalArgumentException("interface Class expected");
        }
        if (! isImplemented(thiz, iface)) {
            return null;
        }
        AccessControlContext accCtxt = AccessController.getContext();
        return iface.cast(Proxy.newProxyInstance(iface.getClassLoader(),
                new Class[]{iface},
                new InterfaceImplementorInvocationHandler(thiz, accCtxt)));
    }

    protected boolean isImplemented(Object thiz, Class<?> iface) {
        return true;
    }

    // called to convert method result after invoke
    protected Object convertResult(Method method, Object res)
            throws ScriptException {
        // default is identity conversion
        return res;
    }

    // called to convert method arguments before invoke
    protected Object[] convertArguments(Method method, Object[] args)
            throws ScriptException {
        // default is identity conversion
        return args;
    }
}