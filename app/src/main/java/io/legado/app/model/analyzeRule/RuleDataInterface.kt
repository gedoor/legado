package io.legado.app.model.analyzeRule

interface RuleDataInterface {

    val variableMap: HashMap<String, String>

    fun putVariable(key: String, value: String?) {
        when {
            value == null -> {
                variableMap.remove(key)
            }
            value.length < 1000 -> {
                variableMap[key] = value
            }
            else -> {

            }
        }
    }

    fun getVariable(key: String): String? {
        return variableMap[key]
    }

}