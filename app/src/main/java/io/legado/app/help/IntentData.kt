package io.legado.app.help

object IntentData {

    private val bigData: MutableMap<String, Any> = mutableMapOf()

    @Synchronized
    fun put(key: String, data: Any?): String {
        data?.let {
            bigData[key] = data
        }
        return key
    }

    @Synchronized
    fun put(data: Any?): String {
        val key = System.currentTimeMillis().toString()
        data?.let {
            bigData[key] = data
        }
        return key
    }

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun <T> get(key: String?): T? {
        if (key == null) return null
        val data = bigData[key]
        bigData.remove(key)
        return data as? T
    }
}