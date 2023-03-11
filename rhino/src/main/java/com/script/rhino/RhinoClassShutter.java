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

package com.script.rhino;

import org.mozilla.javascript.ClassShutter;

import java.util.HashMap;
import java.util.Map;

/**
 * This class prevents script access to certain sensitive classes.
 * Note that this class checks over and above SecurityManager. i.e., although
 * a SecurityManager would pass, class shutter may still prevent access.
 *
 * @author A. Sundararajan
 * @since 1.6
 */
final class RhinoClassShutter implements ClassShutter {
    private static Map<String, Boolean> protectedClasses;
    private static RhinoClassShutter theInstance;

    private RhinoClassShutter() {
    }

    static synchronized ClassShutter getInstance() {
        if (theInstance == null) {
            theInstance = new RhinoClassShutter();
            protectedClasses = new HashMap<String, Boolean>();

            // For now, we just have AccessController. Allowing scripts
            // to this class will allow it to execute doPrivileged in
            // bootstrap context. We can add more classes for other reasons.
            protectedClasses.put("java.lang.Class", Boolean.TRUE);
            protectedClasses.put("java.lang.Runtime", Boolean.TRUE);
            protectedClasses.put("java.io.File", Boolean.TRUE);
            protectedClasses.put("java.security.AccessController", Boolean.TRUE);
        }
        return theInstance;
    }

    public boolean visibleToScripts(String fullClassName) {
        // first do the security check.
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            int i = fullClassName.lastIndexOf(".");
            if (i != -1) {
                try {
                    sm.checkPackageAccess(fullClassName.substring(0, i));
                } catch (SecurityException se) {
                    return false;
                }
            }
        }
        // now, check is it a protected class.
        return protectedClasses.get(fullClassName) == null;
    }
}
