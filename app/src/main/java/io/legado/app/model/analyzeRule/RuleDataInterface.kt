package io.legado.app.model.analyzeRule

interface RuleDataInterface {

    val variableMap: HashMap<String, String>

    fun putVariable(key: String, value: String?) {
        if (value == null) {
            variableMap.remove(key)
        } else if (value.length < 1000) {
            variableMap[key] = value
        }
    }

    fun getVariable(key: String): String? {
        return variableMap[key]
    }

}