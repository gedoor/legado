package io.legado.app.model.analyzeRule

class RuleData : RuleDataInterface {

    override val variableMap by lazy {
        hashMapOf<String, String>()
    }

    override fun putVariable(key: String, value: String) {
        variableMap[key] = value
    }

}