package io.legado.app.model.analyzeRule

interface RuleDataInterface {

    val variableMap: HashMap<String, String>

    fun putVariable(key: String, value: String?): Boolean {
        val keyExist = variableMap.contains(key)
        return when {
            value == null -> {
                variableMap.remove(key)
                putBigVariable(key, null)
                keyExist
            }

            value.length < 10000 -> {
                putBigVariable(key, null)
                variableMap[key] = value
                true
            }

            else -> {
                variableMap.remove(key)
                putBigVariable(key, value)
                keyExist
            }
        }
    }

    fun putBigVariable(key: String, value: String?)

    fun getVariable(key: String): String {
        return variableMap[key] ?: getBigVariable(key) ?: ""
    }

    fun getBigVariable(key: String): String?

}