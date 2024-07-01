/*
 * Decompiled with CFR 0.152.
 */
package com.script

class SimpleBindings @JvmOverloads constructor(
    private val map: MutableMap<String, Any?> = HashMap()
) : Bindings {

    override fun put(key: String, value: Any?): Any? {
        return map.put(key, value)
    }

    override fun putAll(from: Map<out String, Any?>) {
        map.putAll(from)
    }

    override fun clear() {
        map.clear()
    }

    override fun containsKey(key: String): Boolean {
        return map.containsKey(key)
    }

    override fun containsValue(value: Any?): Boolean {
        return map.containsValue(value)
    }

    override val entries: MutableSet<MutableMap.MutableEntry<String, Any?>>
        get() = map.entries

    override operator fun get(key: String): Any? {
        return map[key]
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override val keys: MutableSet<String>
        get() = map.keys

    override fun remove(key: String): Any? {
        return map.remove(key)
    }

    override val size: Int
        get() = map.size

    override val values: MutableCollection<Any?>
        get() = map.values

}