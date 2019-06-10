package io.legado.app.data.entities.rule

import io.legado.app.utils.safeTrim
import io.legado.app.utils.splitNotBlank

data class Rule(
    var selectors: List<BaseRule>,
    var regex: String?,
    var replacement: String?,
    var javascript: String?,
    var extra: String?
) {
    companion object {
        val JS_PATTERN = Regex("""\{\{([^}]+?)}}""")
        val CONST_PATTERN = Regex("""\{(\$\.[^}]+?)}""")

        fun parse(input: String) = when {
            input.startsWith("$.") -> parseJSON(input)
            input.startsWith("//") -> parseXPATH(input)
            input.startsWith("RE:") -> parseREGEX(input)
            isJsRule(input) -> parseJS(input)
            isConstRule(input) -> parseCONST(input)
            else -> parseCSS(input)
        }

        private fun isJsRule(input: String): Boolean {
            val open = input.indexOf("{{")
            if (open < 0) return false
            val close = input.indexOf("}}", open)
            return close > 0
        }

        private fun isConstRule(input: String): Boolean {
            val open = input.indexOf("{")
            if (open < 0) return false
            val close = input.indexOf("}", open)
            return close > 0
        }


        private fun parseCSS(rawRule: String): List<BaseRule> {
            val rules = mutableListOf<BaseRule>()
            for (line in rawRule.splitNotBlank("\n")) {
                val baseRule = BaseRule(type = RuleType.CSS)
                if (line.contains("@@")) {
                    val temp = line.split("@@")
                    baseRule.selector = temp[0].trim()
                    baseRule.attr = temp[1].safeTrim() ?: "text"    // 写了 @@ 但是后面空白的也默认为 text
                } else {
                    baseRule.selector = line
                    baseRule.attr = "text"
                }
                rules.add(baseRule)
            }
            return rules
        }

        private fun parseJSON(rawRule: String): List<BaseRule> {
            val rules = mutableListOf<BaseRule>()
            for (line in rawRule.splitNotBlank("\n")) {
                val baseRule = BaseRule(type = RuleType.JSON)
                baseRule.selector = line
                rules.add(baseRule)
            }
            return rules
        }

        private fun parseXPATH(rawRule: String): List<BaseRule> {
            val rules = mutableListOf<BaseRule>()
            for (line in rawRule.splitNotBlank("\n")) {
                val baseRule = BaseRule(type = RuleType.XPATH)
                baseRule.selector = line
                rules.add(baseRule)
            }
            return rules
        }

        private fun parseCONST(rawRule: String): List<BaseRule> {
            val rules = mutableListOf<BaseRule>()
            val subRule = mutableListOf<String>()
            for (line in rawRule.splitNotBlank("\n")) {
                subRule.clear()
                val baseRule = BaseRule(type = RuleType.JSON)
                // 保留 URL 中的 % 字符
                baseRule.template = CONST_PATTERN.replace(line.replace("%", "%%")) { match ->
                    subRule.add(match.groupValues[1])
                    "%s"
                }
                baseRule.selector = subRule.joinToString("\n")
                rules.add(baseRule)
            }
            return rules
        }

        private fun parseJS(rawRule: String): List<BaseRule> {
            TODO()
        }

        private fun parseREGEX(rawRule: String): List<BaseRule> {
            TODO()
        }

    }
}

/*
*
* CSS 规则说明
* 使用 JSOUP 的"选择规则"，后面加上需要获取的属性名或者方法，如 href, data-url, text 等，如果不加默认为 text
* CSS "选择规则" 和 "属性方法" 之间用 @@ 隔开
*
* CONST 规则说明：
* {$.xxx} 表示要获取 JSON 变量 $.xxx，最先解析
* {@.yyy} 表示要获取之前存储的变量 yyy
* {#.zzz} 表示直接输出 zzz，可以留空，{#.} 什么也不输出
*
* */
enum class RuleType {
    CSS, XPATH, JSON, REGEX, CONST, JS, HYBRID
}
