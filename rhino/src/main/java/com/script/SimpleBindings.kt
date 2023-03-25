/*
 * Decompiled with CFR 0.152.
 */
package com.script

class SimpleBindings @JvmOverloads constructor(
    private val map: MutableMap<String, Any?> = HashMap()
) : Bindings {

    override fun put(key: String, value: Any?): Any? {
        checkKey(key)
        return map.put(key, value)
    }

    override fun putAll(from: Map<out String, Any?>) {
        for ((key, value) in from) {
            checkKey(key)
            this[key] = value
        }
    }

    override fun clear() {
        map.clear()
    }

    override fun containsKey(key: String): Boolean {
        checkKey(key)
        return map.containsKey(key)
    }

    override fun containsValue(value: Any?): Boolean {
        return map.containsValue(value)
    }

    override val entries: MutableSet<MutableMap.MutableEntry<String, Any?>>
        get() = map.entries

    override operator fun get(key: String): Any? {
        checkKey(key)
        return map[key]
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override val keys: MutableSet<String>
        get() = map.keys

    override fun remove(key: String): Any? {
        checkKey(key)
        return map.remove(key)
    }

    override val size: Int
        get() = map.size

    override val values: MutableCollection<Any?>
        get() = map.values

    private fun checkKey(key: Any?) {
        if (key == null) {
            throw NullPointerException("key can not be null")
        }
        if (key !is String) {
            throw ClassCastException("key should be a String")
        }
        require(key != "") { "key can not be empty" }
    }
}