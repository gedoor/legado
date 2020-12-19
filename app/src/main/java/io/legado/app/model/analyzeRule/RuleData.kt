package io.legado.app.model.analyzeRule

interface RuleData {

    val variableMap: HashMap<String, String>

    fun putVariable(key: String, value: String)

}