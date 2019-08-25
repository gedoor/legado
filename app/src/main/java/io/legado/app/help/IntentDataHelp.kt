package io.legado.app.help

object IntentDataHelp {

    private val bigData: MutableMap<String, Any> = mutableMapOf()

    fun putData(key: String, data: Any) {
        bigData[key] = data
    }

    fun getData(key: String): Any? {
        val data = bigData[key]
        bigData.remove(key)
        return data
    }
}