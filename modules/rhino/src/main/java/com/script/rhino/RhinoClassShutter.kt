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
package com.script.rhino

import android.os.Build
import org.mozilla.javascript.ClassShutter
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.reflect.Member
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.Collections

/**
 * This class prevents script access to certain sensitive classes.
 * Note that this class checks over and above SecurityManager. i.e., although
 * a SecurityManager would pass, class shutter may still prevent access.
 *
 * @author A. Sundararajan
 * @since 1.6
 */
object RhinoClassShutter : ClassShutter {

    private val protectedClasses by lazy {
        hashSetOf(
            "java.lang.Class",
            "java.lang.ClassLoader",
            "java.net.URLClassLoader",
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.lang.ProcessImpl",
            "java.lang.UNIXProcess",
            "java.io.File",
            "java.io.FileInputStream",
            "java.io.FileOutputStream",
            "java.io.PrintStream",
            "java.io.FileReader",
            "java.io.FileWriter",
            "java.io.PrintWriter",
            "java.io.UnixFileSystem",
            "java.io.RandomAccessFile",
            "java.io.ObjectInputStream",
            "java.io.ObjectOutputStream",
            "java.security.AccessController",
            "java.nio.file.Paths",
            "java.nio.file.Files",
            "java.nio.file.FileSystems",
            "java.util.Formatter",
            "sun.misc.Unsafe",
            "android.content.Intent",
            "android.provider.Settings",
            "android.app.ActivityThread",
            "android.app.AppGlobals",
            "android.os.Looper",

            "cn.hutool.core.lang.JarClassLoader",
            "cn.hutool.core.lang.Singleton",
            "cn.hutool.core.util.RuntimeUtil",
            "cn.hutool.core.util.ClassLoaderUtil",
            "cn.hutool.core.util.ReflectUtil",
            "cn.hutool.core.util.SerializeUtil",
            "cn.hutool.core.util.ClassUtil",
            "org.mozilla.javascript.DefiningClassLoader",
            "io.legado.app.data.AppDatabase",
            "io.legado.app.data.AppDatabase_Impl",
            "io.legado.app.data.AppDatabaseKt",
            "io.legado.app.utils.ContextExtensionsKt",
            "androidx.core.content.FileProvider",
            "splitties.init.AppCtxKt",
            "okio.JvmSystemFileSystem",
            "okio.JvmFileHandle",
            "okio.NioSystemFileSystem",
            "okio.NioFileSystemFileHandle",
            "okio.Path",

            "android.system",
            "android.database",
            "androidx.sqlite.db",
            "androidx.room",
            "cn.hutool.core.io",
            "cn.hutool.core.bean",
            "cn.hutool.core.lang.reflect",
            "dalvik.system",
            "java.nio.file",
            "java.lang.reflect",
            "java.lang.invoke",
            "io.legado.app.data.dao",
            "com.script",
            "org.mozilla",
            "sun",
        ).let { Collections.unmodifiableSet(it) }
    }

    private val systemClassProtectedName by lazy {
        Collections.unmodifiableSet(hashSetOf("load", "loadLibrary", "exit"))
    }

    fun visibleToScripts(obj: Any): Boolean {
        when (obj) {
            is ClassLoader,
            is Class<*>,
            is Member,
            is Context,
            is ObjectInputStream,
            is ObjectOutputStream,
            is okio.FileSystem,
            is okio.FileHandle,
            is okio.Path,
            is android.content.Context -> return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (obj) {
                is FileSystem,
                is Path -> return false
            }
        }
        return visibleToScripts(obj.javaClass.name)
    }

    fun wrapJavaClass(scope: Scriptable, javaClass: Class<*>): Scriptable {
        return when (javaClass) {
            System::class.java -> {
                ProtectedNativeJavaClass(scope, javaClass, systemClassProtectedName)
            }

            else -> ProtectedNativeJavaClass(scope, javaClass)
        }
    }

    override fun visibleToScripts(fullClassName: String): Boolean {
        if (!protectedClasses.contains(fullClassName)) {
            var className = fullClassName
            while (className.contains(".")) {
                className = className.substringBeforeLast(".")
                if (protectedClasses.contains(className)) {
                    return false
                }
            }
            return true
        }
        return false
    }

}