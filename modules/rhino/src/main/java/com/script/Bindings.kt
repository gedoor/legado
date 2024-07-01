/*
 * Decompiled with CFR 0.152.
 */
package com.script

interface Bindings : MutableMap<String, Any?> {

    override fun containsKey(key: String): Boolean

    override operator fun get(key: String): Any?

    override fun put(key: String, value: Any?): Any?

    override fun putAll(from: Map<out String, *>)

    override fun remove(key: String): Any?

}