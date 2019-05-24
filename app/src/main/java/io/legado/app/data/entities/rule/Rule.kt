package io.legado.app.data.entities.rule

data class Rule (
    var selectors: List<BaseRule>,
    var regex: String?,
    var replacement: String?,
    var javascript: String?,
    var extra: String?
) {
    companion object {
        fun parse(input: String) = when {
            input.startsWith("$.") -> parseJSON(input)
            input.startsWith("//") -> parseXPATH(input)
            input.startsWith("RE:") -> parseREGEX(input)
            input.contains("{{") && input.contains("}}") -> parseJS(input)
            input.contains("{") && input.contains("}") -> parseCONST(input)
            else -> parseCSS(input)
        }

        private fun parseCSS(rawRule: String): List<BaseRule> {
            TODO()
        }

        private fun parseJSON(rawRule: String): List<BaseRule> {
            TODO()
        }

        private fun parseXPATH(rawRule: String): List<BaseRule> {
            TODO()
        }

        private fun parseREGEX(rawRule: String): List<BaseRule> {
            TODO()
        }

        private fun parseCONST(rawRule: String): List<BaseRule> {
            TODO()
        }

        private fun parseJS(rawRule: String): List<BaseRule> {
            TODO()
        }

    }
}

enum class RuleType {
    CSS, XPATH, JSON, REGEX, CONST, JS, HYBRID
}