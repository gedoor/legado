package io.legado.app.model.analyzeRule

import android.text.TextUtils
import androidx.annotation.Keep
import io.legado.app.utils.splitNotBlank
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.seimicrawler.xpath.JXDocument
import org.seimicrawler.xpath.JXNode
import java.util.*

@Keep
class AnalyzeByXPath {
    private var jxDocument: JXDocument? = null
    private var jxNode: JXNode? = null

    fun parse(doc: Any): AnalyzeByXPath {
        if (doc is JXNode) {
            jxNode = doc
            if (jxNode?.isElement == false) {
                jxDocument = strToJXDocument(doc.toString())
                jxNode = null
            }
        } else if (doc is Document) {
            jxDocument = JXDocument.create(doc)
            jxNode = null
        } else if (doc is Element) {
            jxDocument = JXDocument.create(Elements(doc))
            jxNode = null
        } else if (doc is Elements) {
            jxDocument = JXDocument.create(doc)
            jxNode = null
        } else {
            jxDocument = strToJXDocument(doc.toString())
            jxNode = null
        }
        return this
    }

    private fun strToJXDocument(html: String): JXDocument {
        var html1 = html
        if (html1.endsWith("</td>")) {
            html1 = String.format("<tr>%s</tr>", html1)
        }
        if (html1.endsWith("</tr>") || html1.endsWith("</tbody>")) {
            html1 = String.format("<table>%s</table>", html1)
        }
        return JXDocument.create(html1)
    }

    internal fun getElements(xPath: String): List<JXNode>? {
        if (TextUtils.isEmpty(xPath)) {
            return null
        }
        val jxNodes = ArrayList<JXNode>()
        val elementsType: String
        val rules: Array<String>
        when {
            xPath.contains("&&") -> {
                rules = xPath.splitNotBlank("&&")
                elementsType = "&"
            }
            xPath.contains("%%") -> {
                rules = xPath.splitNotBlank("%%")
                elementsType = "%"
            }
            else -> {
                rules = xPath.splitNotBlank("||")
                elementsType = "|"
            }
        }
        if (rules.size == 1) {
            return jxNode?.sel(rules[0]) ?: jxDocument?.selN(rules[0])
        } else {
            val results = ArrayList<List<JXNode>>()
            for (rl in rules) {
                val temp = getElements(rl)
                if (temp != null && temp.isNotEmpty()) {
                    results.add(temp)
                    if (temp.isNotEmpty() && elementsType == "|") {
                        break
                    }
                }
            }
            if (results.size > 0) {
                if ("%" == elementsType) {
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
        val elementsType: String
        val rules: Array<String>
        when {
            xPath.contains("&&") -> {
                rules = xPath.splitNotBlank("&&")
                elementsType = "&"
            }
            xPath.contains("%%") -> {
                rules = xPath.splitNotBlank("%%")
                elementsType = "%"
            }
            else -> {
                rules = xPath.splitNotBlank("||")
                elementsType = "|"
            }
        }
        if (rules.size == 1) {
            val jxNodes = jxNode?.sel(xPath) ?: jxDocument?.selN(xPath)
            jxNodes?.map {
                result.add(it.asString())
            }
            return result
        } else {
            val results = ArrayList<List<String>>()
            for (rl in rules) {
                val temp = getStringList(rl)
                if (temp.isNotEmpty()) {
                    results.add(temp)
                    if (temp.isNotEmpty() && elementsType == "|") {
                        break
                    }
                }
            }
            if (results.size > 0) {
                if ("%" == elementsType) {
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
        val rules: Array<String>
        val elementsType: String
        if (rule.contains("&&")) {
            rules = rule.splitNotBlank("&&")
            elementsType = "&"
        } else {
            rules = rule.splitNotBlank("||")
            elementsType = "|"
        }
        if (rules.size == 1) {
            val jxNodes = jxNode?.sel(rule) ?: jxDocument?.selN(rule)
            jxNodes?.let {
                return TextUtils.join("\n", jxNodes)
            }
            return null
        } else {
            val textList = arrayListOf<String>()
            for (rl in rules) {
                val temp = getString(rl)
                if (!temp.isNullOrEmpty()) {
                    textList.add(temp)
                    if (elementsType == "|") {
                        break
                    }
                }
            }
            return textList.joinToString("\n")
        }
    }
}
