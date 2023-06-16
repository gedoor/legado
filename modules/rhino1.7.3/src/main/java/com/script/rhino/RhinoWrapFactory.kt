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
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.WrapFactory
import java.lang.reflect.Member

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
        val classShutter = RhinoClassShutter
        return when (javaObject) {
            is ClassLoader,
            is Class<*>,
            is Member,
            is android.content.Context -> {
                null
            }

            else -> {
                val name = javaObject.javaClass.name
                if (classShutter.visibleToScripts(name)) {
                    super.wrapAsJavaObject(cx, scope, javaObject, staticType)
                } else {
                    null
                }
            }
        }
    }

}