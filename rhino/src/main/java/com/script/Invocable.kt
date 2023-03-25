/*
 * Decompiled with CFR 0.152.
 */
package com.script

interface Invocable {
    fun <T> getInterface(clazz: Class<T>): T?

    fun <T> getInterface(obj: Any?, paramClass: Class<T>): T?

    @Throws(ScriptException::class, NoSuchMethodException::class)
    fun invokeFunction(name: String, vararg args: Any): Any?

    @Throws(ScriptException::class, NoSuchMethodException::class)
    fun invokeMethod(obj: Any?, name: String, vararg args: Any): Any?
}