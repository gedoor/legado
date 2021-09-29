package io.legado.app.help

object IntentData {

    private val bigData: MutableMap<String, Any> = mutableMapOf()

    fun put(data: Any, tag: String = ""): String {
        val key = tag + System.currentTimeMillis()
        bigData[key] = data
        return key
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String?): T? {
        if (key == null) return null
        val data = bigData[key]
        bigData.remove(key)
        return data as? T
    }
}