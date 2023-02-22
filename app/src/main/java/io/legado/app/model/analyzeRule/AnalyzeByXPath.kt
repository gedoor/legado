package io.legado.app.model.analyzeRule

import android.text.TextUtils
import androidx.annotation.Keep
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements

@Keep
class AnalyzeByXPath(doc: Any) {

    private var element: Element = parse(doc)

    private fun parse(doc: Any): Element {
        return when (doc) {
            is Element -> doc
            else -> Jsoup.parse(doc.toString())
        }
    }

    internal fun getElements(xPath: String): Elements? {

        if (xPath.isEmpty()) return null

        val jxNodes = Elements()
        val ruleAnalyzes = RuleAnalyzer(xPath)
        val rules = ruleAnalyzes.splitRule("&&", "||", "%%")

        if (rules.size == 1) {
            return element.selectXpath(rules[0])
        } else {
            val results = ArrayList<Elements>()
            for (rl in rules) {
                val temp = getElements(rl)
                if (temp != null && temp.isNotEmpty()) {
                    results.add(temp)
                    if (temp.isNotEmpty() && ruleAnalyzes.elementsType == "||") {
                        break
                    }
                }
            }
            if (results.size > 0) {
                if ("%%" == ruleAnalyzes.elementsType) {
                    for (i in results[0].indices) {
                        for (temp in results) {
                            if (i < temp.size) {
                                jxNodes.add(temp[i])
                            }
                        }
                    }
                } else {
                    for (temp in results) {
                        jxNodes.addAll(temp)
                    }
                }
            }
        }
        return jxNodes
    }

    internal fun getStringList(xPath: String): List<String> {

        val result = ArrayList<String>()
        val ruleAnalyzes = RuleAnalyzer(xPath)
        val rules = ruleAnalyzes.splitRule("&&", "||", "%%")

        if (rules.size == 1) {
            element.selectXpath(xPath).forEach {
                result.add(it.toString())
            }
            return result
        } else {
            val results = ArrayList<List<String>>()
            for (rl in rules) {
                val temp = getStringList(rl)
                if (temp.isNotEmpty()) {
                    results.add(temp)
                    if (temp.isNotEmpty() && ruleAnalyzes.elementsType == "||") {
                        break
                    }
                }
            }
            if (results.size > 0) {
                if ("%%" == ruleAnalyzes.elementsType) {
                    for (i in results[0].indices) {
                        for (temp in results) {
                            if (i < temp.size) {
                                result.add(temp[i])
                            }
                        }
                    }
                } else {
                    for (temp in results) {
                        result.addAll(temp)
                    }
                }
            }
        }
        return result
    }

    fun getString(rule: String): String? {
        val ruleAnalyzes = RuleAnalyzer(rule)
        val rules = ruleAnalyzes.splitRule("&&", "||")
        if (rules.size == 1) {
            val xpath = when {
                rule.startsWith("///") -> ".${rule.substring(1)}"
                rule.startsWith("/") -> ".$rule"
                else -> rule
            }
            val x = xpath.substringAfterLast("/")
            return if (x.startsWith("@")) {
                element.selectXpath(xpath.substringBeforeLast("/"))
                    .eachAttr(x.substring(1)).let {
                        TextUtils.join("\n", it)
                    }
            } else {
                element.selectXpath(xpath, TextNode::class.java).let {
                    TextUtils.join("\n", it)
                }
            }
        } else {
            val textList = arrayListOf<String>()
            for (rl in rules) {
                val temp = getString(rl)
                if (!temp.isNullOrEmpty()) {
                    textList.add(temp)
                    if (ruleAnalyzes.elementsType == "||") {
                        break
                    }
                }
            }
            return textList.joinToString("\n")
        }
    }
}
