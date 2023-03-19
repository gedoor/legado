package io.legado.app.model.analyzeRule

interface RuleDataInterface {

    val variableMap: HashMap<String, String>

    fun putVariable(key: String, value: String?): Boolean {
        return when {
            value == null -> {
                variableMap.remove(key)
                putBigVariable(key, null)
                true
            }
            value.length < 10000 -> {
                variableMap[key] = value
                true
            }
            else -> {
                putBigVariable(key, value)
                false
            }
        }
    }

    fun putBigVariable(key: String, value: String?)

    fun getVariable(key: String): String {
        return variableMap[key] ?: getBigVariable(key) ?: ""
    }

    fun getBigVariable(key: String): String?

}