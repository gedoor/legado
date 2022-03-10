package io.legado.app.model.analyzeRule

class RuleData : RuleDataInterface {

    override var variable: String? = null

    override val variableMap by lazy {
        hashMapOf<String, String>()
    }

    override fun putBigVariable(key: String, value: String) {
        variableMap[key] = value
    }

}