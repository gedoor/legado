package io.legado.app.data.entities.rule

data class BaseRule(
    var selector: String = "",
    var template: String? = null,
    var attr: String? = null,
    var type: RuleType
)
