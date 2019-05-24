package io.legado.app.data.entities.rule

data class PutRule (
    var selector: BaseRule,
    var type: RuleType,
    var key: String
)