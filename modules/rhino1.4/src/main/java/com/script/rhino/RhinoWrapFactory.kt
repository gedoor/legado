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

import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeJavaObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.WrapFactory
import java.lang.reflect.Member
import java.lang.reflect.Modifier

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
object RhinoWrapFactory : WrapFactory() {

    override fun wrapAsJavaObject(
        cx: Context,
        scope: Scriptable?,
        javaObject: Any,
        staticType: Class<*>?
    ): Scriptable? {
        scope?.delete("Packages")
        val sm = System.getSecurityManager()
        val classShutter = RhinoClassShutter
        return if (javaObject is ClassLoader) {
            sm?.checkPermission(RuntimePermission("getClassLoader"))
            super.wrapAsJavaObject(cx, scope, javaObject, staticType)
        } else {
            var name: String? = null
            if (javaObject is Class<*>) {
                name = javaObject.name
            } else if (javaObject is Member) {
                if (sm != null && !Modifier.isPublic(javaObject.modifiers)) {
                    return null
                }
                name = javaObject.declaringClass.name
            }
            if (name != null) {
                if (!classShutter.visibleToScripts(name)) null else super.wrapAsJavaObject(
                    cx,
                    scope,
                    javaObject,
                    staticType
                )
            } else {
                var dynamicType: Class<*>? = javaObject.javaClass
                name = dynamicType!!.name
                if (classShutter.visibleToScripts(name)) {
                    super.wrapAsJavaObject(cx, scope, javaObject, staticType)
                } else {
                    var type: Class<*>? = null
                    if (staticType != null && staticType.isInterface) {
                        type = staticType
                    } else {
                        while (dynamicType != null) {
                            dynamicType = dynamicType.superclass
                            name = dynamicType.name
                            if (classShutter.visibleToScripts(name)) {
                                type = dynamicType
                                break
                            }
                        }
                        assert(type != null) { "java.lang.Object 不可访问" }
                    }
                    RhinoJavaObject(scope, javaObject, type)
                }
            }
        }
    }

    private class RhinoJavaObject(
        scope: Scriptable?,
        obj: Any?,
        type: Class<*>?
    ) : NativeJavaObject(scope, null, type) {
        init {
            javaObject = obj
        }

        override fun get(name: String, start: Scriptable): Any {
            return if (name != "getClass" && name != "exec") super.get(name, start) else NOT_FOUND
        }
    }

}