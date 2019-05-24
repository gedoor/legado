package io.legado.app.data.entities.rule

data class Rule (
    var selectors: List<BaseRule>,
    var mode: RuleMode,
    var type: RuleType,
    var regex: String?,
    var replacement: String?,
    var javascript: String?,
    var extra: String?
)

enum class RuleMode {
    FIRST, ALL, CROSS
}

enum class RuleType {
    CSS, XPATH, JSON, REGEX, CONST, JS, HYBRID
}