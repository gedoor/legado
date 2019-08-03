package io.legado.app.model.analyzeRule

import android.text.TextUtils
import io.legado.app.utils.splitNotBlank
import org.jsoup.nodes.Document
import org.seimicrawler.xpath.JXDocument
import org.seimicrawler.xpath.JXNode
import java.util.*

class AnalyzeByXPath {
    private var jxDocument: JXDocument? = null

    fun parse(doc: Any): AnalyzeByXPath {
        if (doc is Document) {
            jxDocument = JXDocument.create(doc)
        } else {
            var html = doc.toString()
            // 给表格标签添加完整的框架结构,否则会丢失表格标签;html标准不允许表格标签独立在table之外
            if (html.endsWith("</td>")) {
                html = String.format("<tr>%s</tr>", html)
            }
            if (html.endsWith("</tr>") || html.endsWith("</tbody>")) {
                html = String.format("<table>%s</table>", html)
            }
            jxDocument = JXDocument.create(html)
        }
        return this
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
            return jxDocument?.selN(rules[0])
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
                    for (i in 0 until results[0].size) {
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
            val jxNodes = jxDocument!!.selN(xPath)
            for (jxNode in jxNodes) {
                /*if(jxNode.isString()){
                    result.add(String.valueOf(jxNode));
                }*/
                result.add(jxNode.toString())
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
                    for (i in 0 until results[0].size) {
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
            val jxNodes = jxDocument?.selN(rule)
            jxNodes?.let {
                TextUtils.join(",", jxNodes)
            }
            return null
        } else {
            val sb = StringBuilder()
            for (rl in rules) {
                val temp = getString(rl)
                if (!TextUtils.isEmpty(temp)) {
                    sb.append(temp)
                    if (elementsType == "|") {
                        break
                    }
                }
            }
            return sb.toString()
        }
    }
}
