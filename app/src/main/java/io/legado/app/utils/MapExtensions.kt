package io.legado.app.utils

fun HashMap<String, *>.has(key: String, ignoreCase: Boolean = false): Boolean {
    for (item in this) {
        if (key.equals(item.key, ignoreCase)) {
            return true
        }
    }
    return false
}

fun HashMap<String, *>.get(key: String, ignoreCase: Boolean = false): Any? {
    for (item in this) {
        if (key.equals(item.key, ignoreCase)) {
            return item.value
        }
    }
    return null
}