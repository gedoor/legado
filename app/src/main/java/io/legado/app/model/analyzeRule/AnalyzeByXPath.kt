package io.legado.app.model.analyzeRule

import android.text.TextUtils
import androidx.annotation.Keep
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.jsoup.select.Elements
import org.seimicrawler.xpath.JXDocument
import org.seimicrawler.xpath.JXNode

@Keep
class AnalyzeByXPath(doc: Any) {
    private var jxNode: Any = parse(doc)

    private fun parse(doc: Any): Any {
        return when (doc) {
            is JXNode -> if (doc.isElement) doc else strToJXDocument(doc.toString())
            is Document -> JXDocument.create(doc)
            is Element -> JXDocument.create(Elements(doc))
            is Elements -> JXDocument.create(doc)
            else -> strToJXDocument(doc.toString())
        }
    }

    private fun strToJXDocument(html: String): JXDocument {
        var html1 = html
        if (html1.endsWith("</td>")) {
            html1 = "<tr>${html1}</tr>"
        }
        if (html1.endsWith("</tr>") || html1.endsWith("</tbody>")) {
            html1 = "<table>${html1}</table>"
        }
        kotlin.runCatching {
            if (html1.trim().startsWith("<?xml", true)) {
                return JXDocument.create(Jsoup.parse(html1, Parser.xmlParser()))
            }
        }
        return JXDocument.create(html1)
    }

    private fun getResult(xPath: String): List<JXNode>? {
        val node = jxNode
        return if (node is JXNode) {
            node.sel(xPath)
        } else {
            (node as JXDocument).selN(xPath)
        }
    }

    internal fun getElements(xPath: String): List<JXNode>? {

        if (xPath.isEmpty()) return null

        val jxNodes = ArrayList<JXNode>()
        val ruleAnalyzes = RuleAnalyzer(xPath)
        val rules = ruleAnalyzes.splitRule("&&", "||", "%%")

        if (rules.size == 1) {
            return getResult(rules[0])
        } else {
            val results = ArrayList<List<JXNode>>()
            for (rl in rules) {
                val temp = getElements(rl)
                if (!temp.isNullOrEmpty()) {
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
            getResult(xPath)?.map {
                result.add(it.asString())
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
            getResult(rule)?.let {
                return TextUtils.join("\n", it)
            }
            return null
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
