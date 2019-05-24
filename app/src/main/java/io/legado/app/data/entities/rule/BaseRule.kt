package io.legado.app.data.entities.rule

data class BaseRule(
    var selector: String,
    var template: String?,
    var attr: String?,
    var type: RuleType
)
