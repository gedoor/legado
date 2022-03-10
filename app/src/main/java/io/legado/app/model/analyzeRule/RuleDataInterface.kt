package io.legado.app.model.analyzeRule

import io.legado.app.utils.GSON

interface RuleDataInterface {

    var variable: String?

    val variableMap: HashMap<String, String>

    fun putVariable(key: String, value: String?) {
        when {
            value == null -> {
                variableMap.remove(key)
                variable = GSON.toJson(variableMap)
            }
            value.length < 1000 -> {
                variableMap[key] = value
                variable = GSON.toJson(variableMap)
            }
            else -> {
                variableMap[key] = value
                variable = GSON.toJson(variableMap)
            }
        }
    }

    fun putBigVariable(key: String, value: String)

    fun getVariable(key: String): String? {
        return variableMap[key]
    }

}