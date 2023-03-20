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

import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;


/**
 * This wrap factory is used for security reasons. JSR 223 script
 * engine interface and JavaScript engine classes are run as bootstrap
 * classes. For example, java.lang.Class.forName method (when called without
 * class loader) uses caller's class loader. This may be exploited by script
 * authors to access classes otherwise not accessible. For example,
 * classes in sun.* namespace are normally not accessible to untrusted
 * code and hence should not be accessible to JavaScript run from
 * untrusted code.
 *
 * @author A. Sundararajan
 * @since 1.6
 */
final class RhinoWrapFactory extends WrapFactory {
    private RhinoWrapFactory() {}
    private static RhinoWrapFactory theInstance;

    static synchronized WrapFactory getInstance() {
        if (theInstance == null) {
            theInstance = new RhinoWrapFactory();
        }
        return theInstance;
    }

    // We use instance of this class to wrap security sensitive
    // Java object. Please refer below.
    private static class RhinoJavaObject extends NativeJavaObject {
        RhinoJavaObject(Scriptable scope, Object obj, Class type) {
            // we pass 'null' to object. NativeJavaObject uses
            // passed 'type' to reflect fields and methods when
            // object is null.
            super(scope, null, type);

            // Now, we set actual object. 'javaObject' is protected
            // field of NativeJavaObject.
            javaObject = obj;
        }

        @Override
        public Object get(String name, Scriptable start) {
            if (name.equals("getClass") || name.equals("exec")) {
                return NOT_FOUND;
            }
            return super.get(name, start);
        }
    }

    public Scriptable wrapAsJavaObject(Context cx, Scriptable scope,
            Object javaObject, Class staticType) {
        if (scope != null) {
            scope.delete("Packages");
        }
        SecurityManager sm = System.getSecurityManager();
        ClassShutter classShutter = RhinoClassShutter.getInstance();
        if (javaObject instanceof ClassLoader) {
            // Check with Security Manager whether we can expose a
            // ClassLoader...
            if (sm != null) {
                sm.checkPermission(new RuntimePermission("getClassLoader"));
            }
            // if we fall through here, check permission succeeded.
            return super.wrapAsJavaObject(cx, scope, javaObject, staticType);
        } else {
            String name = null;
            if (javaObject instanceof Class) {
                name = ((Class)javaObject).getName();
            } else if (javaObject instanceof Member) {
                Member member = (Member) javaObject;
                // Check member access. Don't allow reflective access to
                // non-public members. Note that we can't call checkMemberAccess
                // because that expects exact stack depth!
                if (sm != null && !Modifier.isPublic(member.getModifiers())) {
                    return null;
                }
                name = member.getDeclaringClass().getName();
            }
            // Now, make sure that no ClassShutter prevented Class or Member
            // of it is accessed reflectively. Note that ClassShutter may
            // prevent access to a class, even though SecurityManager permit.
            if (name != null) {
                if (!classShutter.visibleToScripts(name)) {
                    return null;
                } else {
                    return super.wrapAsJavaObject(cx, scope, javaObject, staticType);
                }
            }
        }

        // we have got some non-reflective object.
        Class dynamicType = javaObject.getClass();
        String name = dynamicType.getName();
        if (!classShutter.visibleToScripts(name)) {
            // Object of some sensitive class (such as sun.net.www.*
            // objects returned from public method of java.net.URL class.
            // We expose this object as though it is an object of some
            // super class that is safe for access.

            Class type = null;

            // Whenever a Java Object is wrapped, we are passed with a
            // staticType which is the type found from environment. For
            // example, method return type known from signature. The dynamic
            // type would be the actual Class of the actual returned object.
            // If the staticType is an interface, we just use that type.
            if (staticType != null && staticType.isInterface()) {
                type = staticType;
            } else {
                // dynamicType is always a class type and never an interface.
                // find an accessible super class of the dynamic type.
                while (dynamicType != null) {
                    dynamicType = dynamicType.getSuperclass();
                    name = dynamicType.getName();
                    if (classShutter.visibleToScripts(name)) {
                         type = dynamicType; break;
                    }
                }
                // atleast java.lang.Object has to be accessible. So, when
                // we reach here, type variable should not be null.
                assert type != null:
                       "even java.lang.Object is not accessible?";
            }
            // create custom wrapper with the 'safe' type.
            return new RhinoJavaObject(scope, javaObject, type);
        } else {
            return super.wrapAsJavaObject(cx, scope, javaObject, staticType);
        }
    }
}
