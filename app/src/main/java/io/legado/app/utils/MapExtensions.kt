package io.legado.app.utils

fun HashMap<String, *>.has(key: String, ignoreCase: Boolean = false): Boolean {
    for (item in this) {
        if (key.equals(item.key, ignoreCase)) {
            return true
        }
    }
    return false
}

fun <T> HashMap<String, T>.get(key: String, ignoreCase: Boolean = false): T? {
    for (item in this) {
        if (key.equals(item.key, ignoreCase)) {
            return item.value
        }
    }
    return null
}

inline fun <K, V> MutableMap<K, V>.getOrPutLimit(key: K, maxSize: Int, defaultValue: () -> V): V {
    var value = get(key)
    if (containsKey(key)) {
        @Suppress("UNCHECKED_CAST")
        return value as V
    }
    value = defaultValue()
    if (size < maxSize) {
        put(key, value)
    }
    return value
}
