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

package com.script.rhino;

import com.script.Invocable;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;



/**
 * This class implements Rhino-like JavaAdapter to help implement a Java
 * interface in JavaScript. We support this using Invocable.getInterface.
 * Using this JavaAdapter, script author could write:
 *
 *    var r = new java.lang.Runnable() {
 *                run: function() { script... }
 *            };
 *
 *    r.run();
 *    new java.lang.Thread(r).start();
 *
 * Note that Rhino's JavaAdapter support allows extending a Java class and/or
 * implementing one or more interfaces. This JavaAdapter implementation does
 * not support these.
 *
 * @author A. Sundararajan
 * @since 1.6
 */
final class JavaAdapter extends ScriptableObject implements Function {
    private JavaAdapter(Invocable engine) {
        this.engine = engine;
    }

    static void init(Context cx, Scriptable scope, boolean sealed)
    throws RhinoException {
        RhinoTopLevel topLevel = (RhinoTopLevel) scope;
        Invocable engine = topLevel.getScriptEngine();
        JavaAdapter obj = new JavaAdapter(engine);
        obj.setParentScope(scope);
        obj.setPrototype(getFunctionPrototype(scope));
        /*
         * Note that we can't use defineProperty. A property of this
         * name is already defined in Context.initStandardObjects. We
         * simply overwrite the property value!
         */
        ScriptableObject.putProperty(topLevel, "JavaAdapter", obj);
    }

    public String getClassName() {
        return "JavaAdapter";
    }

    public Object call(Context cx, Scriptable scope, Scriptable thisObj,
            Object[] args) throws RhinoException {
        return construct(cx, scope, args);
    }

    public Scriptable construct(Context cx, Scriptable scope, Object[] args)
    throws RhinoException {
        if (args.length == 2) {
            Class<?> clazz = null;
            Object obj1 = args[0];
            if (obj1 instanceof Wrapper) {
                Object o = ((Wrapper)obj1).unwrap();
                if (o instanceof Class && ((Class)o).isInterface()) {
                    clazz = (Class) o;
                }
            } else if (obj1 instanceof Class) {
                if (((Class)obj1).isInterface()) {
                    clazz = (Class) obj1;
                }
            }
            if (clazz == null) {
                throw Context.reportRuntimeError("JavaAdapter: first arg should be interface Class");
            }

            Scriptable topLevel = ScriptableObject.getTopLevelScope(scope);
            return cx.toObject(engine.getInterface(args[1],  clazz), topLevel);
        } else {
            throw Context.reportRuntimeError("JavaAdapter requires two arguments");
        }
    }

    private Invocable engine;
}
