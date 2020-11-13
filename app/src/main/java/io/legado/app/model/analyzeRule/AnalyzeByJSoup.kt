package io.legado.app.model.analyzeRule

import android.text.TextUtils.isEmpty
import android.text.TextUtils.join
import androidx.annotation.Keep
import io.legado.app.utils.splitNotBlank
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Collector
import org.jsoup.select.Elements
import org.jsoup.select.Evaluator
import org.seimicrawler.xpath.JXNode
import java.util.*

/**
 * Created by GKF on 2018/1/25.
 * 书源规则解析
 */
@Keep
class AnalyzeByJSoup(doc: Any) {
    companion object {
        val validKeys = arrayOf("class", "id", "tag", "text", "children")

        fun parse(doc: Any): Element {
            return when (doc) {
                is Element -> doc
                is JXNode -> if (doc.isElement) doc.asElement() else Jsoup.parse(doc.toString())
                else -> Jsoup.parse(doc.toString())
            }
        }

    }

    private var element: Element = parse(doc)

    /**
     * 获取列表
     */
    internal fun getElements(rule: String): Elements {
        return getElements(element, rule)
    }

    /**
     * 合并内容列表,得到内容
     */
    internal fun getString(ruleStr: String): String? {
        if (isEmpty(ruleStr)) {
            return null
        }
        val textS = getStringList(ruleStr)
        return if (textS.isEmpty()) {
            null
        } else {
            textS.joinToString("\n")
        }

    }

    /**
     * 获取一个字符串
     */
    internal fun getString0(ruleStr: String): String {
        val urlList = getStringList(ruleStr)
        return if (urlList.isNotEmpty()) {
            urlList[0]
        } else ""
    }

    /**
     * 获取所有内容列表
     */
    internal fun getStringList(ruleStr: String): List<String> {
        val textS = ArrayList<String>()
        if (isEmpty(ruleStr)) {
            return textS
        }
        //拆分规则
        val sourceRule = SourceRule(ruleStr)
        if (isEmpty(sourceRule.elementsRule)) {
            textS.add(element.data() ?: "")
        } else {
            val elementsType: String
            val ruleStrS: Array<String>
            when {
                sourceRule.elementsRule.contains("&&") -> {
                    elementsType = "&"
                    ruleStrS = sourceRule.elementsRule.splitNotBlank("&&")
                }
                sourceRule.elementsRule.contains("%%") -> {
                    elementsType = "%"
                    ruleStrS = sourceRule.elementsRule.splitNotBlank("%%")
                }
                else -> {
                    elementsType = "|"
                    ruleStrS = sourceRule.elementsRule.splitNotBlank("||")
                }
            }
            val results = ArrayList<List<String>>()
            for (ruleStrX in ruleStrS) {
                val temp: List<String>?
                temp = if (sourceRule.isCss) {
                    val lastIndex = ruleStrX.lastIndexOf('@')
                    getResultLast(
                        element.select(ruleStrX.substring(0, lastIndex)),
                        ruleStrX.substring(lastIndex + 1)
                    )
                } else {
                    getResultList(ruleStrX)
                }
                if (!temp.isNullOrEmpty()) {
                    results.add(temp)
                    if (results.isNotEmpty() && elementsType == "|") {
                        break
                    }
                }
            }
            if (results.size > 0) {
                if ("%" == elementsType) {
                    for (i in results[0].indices) {
                        for (temp in results) {
                            if (i < temp.size) {
                                textS.add(temp[i])
                            }
                        }
                    }
                } else {
                    for (temp in results) {
                        textS.addAll(temp)
                    }
                }
            }
        }
        return textS
    }

    /**
     * 获取Elements
     */
    private fun getElements(temp: Element?, rule: String): Elements {
        val elements = Elements()
        if (temp == null || isEmpty(rule)) {
            return elements
        }
        val sourceRule = SourceRule(rule)
        val elementsType: String
        val ruleStrS: Array<String>
        when {
            sourceRule.elementsRule.contains("&&") -> {
                elementsType = "&"
                ruleStrS = sourceRule.elementsRule.splitNotBlank("&&")
            }
            sourceRule.elementsRule.contains("%%") -> {
                elementsType = "%"
                ruleStrS = sourceRule.elementsRule.splitNotBlank("%%")
            }
            else -> {
                elementsType = "|"
                ruleStrS = sourceRule.elementsRule.splitNotBlank("||")
            }
        }
        val elementsList = ArrayList<Elements>()
        if (sourceRule.isCss) {
            for (ruleStr in ruleStrS) {
                val tempS = temp.select(ruleStr)
                elementsList.add(tempS)
                if (tempS.size > 0 && elementsType == "|") {
                    break
                }
            }
        } else {
            for (ruleStr in ruleStrS) {
                val tempS = getElementsSingle(temp, ruleStr)
                elementsList.add(tempS)
                if (tempS.size > 0 && elementsType == "|") {
                    break
                }
            }
        }
        if (elementsList.size > 0) {
            if ("%" == elementsType) {
                for (i in 0 until elementsList[0].size) {
                    for (es in elementsList) {
                        if (i < es.size) {
                            elements.add(es[i])
                        }
                    }
                }
            } else {
                for (es in elementsList) {
                    elements.addAll(es)
                }
            }
        }
        return elements
    }

    private fun filterElements(elements: Elements, rules: Array<String>?): Elements {
        if (rules == null || rules.size < 2) return elements
        val result = Elements()
        for (element in elements) {
            var isOk = false
            when (rules[0]) {
                "class" -> isOk = element.getElementsByClass(rules[1]).size > 0
                "id" -> isOk = element.getElementById(rules[1]) != null
                "tag" -> isOk = element.getElementsByTag(rules[1]).size > 0
                "text" -> isOk = element.getElementsContainingOwnText(rules[1]).size > 0
            }
            if (isOk) {
                result.add(element)
            }
        }
        return result
    }

    /**
     * 获取Elements按照一个规则
     */
    private fun getElementsSingle(temp: Element, rule: String): Elements {
        val elements = Elements()
        try {
            val rs = rule.trim { it <= ' ' }.splitNotBlank("@")
            if (rs.size > 1) {
                elements.add(temp)
                for (rl in rs) {
                    val es = Elements()
                    for (et in elements) {
                        es.addAll(getElements(et, rl))
                    }
                    elements.clear()
                    elements.addAll(es)
                }
            } else {
                val rulePcx = rule.split("!")
                val rulePc = rulePcx[0].trim { it <= ' ' }.split(">")
                val rules = rulePc[0].trim { it <= ' ' }.split(".")
                var filterRules: Array<String>? = null
                var needFilterElements = rulePc.size > 1 && !isEmpty(rulePc[1].trim { it <= ' ' })
                if (needFilterElements) {
                    filterRules = rulePc[1].trim { it <= ' ' }.split(".").toTypedArray()
                    filterRules[0] = filterRules[0].trim { it <= ' ' }
                    if (filterRules.size < 2
                        || !validKeys.contains(filterRules[0])
                        || filterRules[1].trim { it <= ' ' }.isEmpty()
                    ) {
                        needFilterElements = false
                    }
                    filterRules[1] = filterRules[1].trim { it <= ' ' }
                }
                when (rules[0]) {
                    "children" -> {
                        var children = temp.children()
                        if (needFilterElements)
                            children = filterElements(children, filterRules)
                        elements.addAll(children)
                    }
                    "class" -> {
                        var elementsByClass = temp.getElementsByClass(rules[1])
                        if (rules.size == 3 && rules[2].isNotEmpty()) {
                            val index = Integer.parseInt(rules[2])
                            if (index < 0) {
                                elements.add(elementsByClass[elementsByClass.size + index])
                            } else {
                                elements.add(elementsByClass[index])
                            }
                        } else {
                            if (needFilterElements)
                                elementsByClass = filterElements(elementsByClass, filterRules)
                            elements.addAll(elementsByClass)
                        }
                    }
                    "tag" -> {
                        var elementsByTag = temp.getElementsByTag(rules[1])
                        if (rules.size == 3 && rules[2].isNotEmpty()) {
                            val index = Integer.parseInt(rules[2])
                            if (index < 0) {
                                elements.add(elementsByTag[elementsByTag.size + index])
                            } else {
                                elements.add(elementsByTag[index])
                            }
                        } else {
                            if (needFilterElements)
                                elementsByTag = filterElements(elementsByTag, filterRules)
                            elements.addAll(elementsByTag)
                        }
                    }
                    "id" -> {
                        var elementsById = Collector.collect(Evaluator.Id(rules[1]), temp)
                        if (rules.size == 3 && rules[2].isNotEmpty()) {
                            val index = Integer.parseInt(rules[2])
                            if (index < 0) {
                                elements.add(elementsById[elementsById.size + index])
                            } else {
                                elements.add(elementsById[index])
                            }
                        } else {
                            if (needFilterElements)
                                elementsById = filterElements(elementsById, filterRules)
                            elements.addAll(elementsById)
                        }
                    }
                    "text" -> {
                        var elementsByText = temp.getElementsContainingOwnText(rules[1])
                        if (needFilterElements)
                            elementsByText = filterElements(elementsByText, filterRules)
                        elements.addAll(elementsByText)
                    }
                    else -> elements.addAll(temp.select(rulePcx[0]))
                }
                if (rulePcx.size > 1) {
                    val rulePcs = rulePcx[1].splitNotBlank(":")
                    for (pc in rulePcs) {
                        val pcInt = Integer.parseInt(pc)
                        if (pcInt < 0 && elements.size + pcInt >= 0) {
                            elements[elements.size + pcInt] = null
                        } else if (Integer.parseInt(pc) < elements.size) {
                            elements[Integer.parseInt(pc)] = null
                        }
                    }
                    val es = Elements()
                    es.add(null)
                    elements.removeAll(es)
                }
            }
        } catch (ignore: Exception) {
        }

        return elements
    }

    /**
     * 获取内容列表
     */
    private fun getResultList(ruleStr: String): List<String>? {
        if (isEmpty(ruleStr)) {
            return null
        }
        var elements = Elements()
        elements.add(element)
        val rules = ruleStr.splitNotBlank("@")
        for (i in 0 until rules.size - 1) {
            val es = Elements()
            for (elt in elements) {
                es.addAll(getElementsSingle(elt, rules[i]))
            }
            elements.clear()
            elements = es
        }
        return if (elements.isEmpty()) {
            null
        } else getResultLast(elements, rules[rules.size - 1])
    }

    /**
     * 根据最后一个规则获取内容
     */
    private fun getResultLast(elements: Elements, lastRule: String): List<String> {
        val textS = ArrayList<String>()
        try {
            when (lastRule) {
                "text" -> for (element in elements) {
                    textS.add(element.text())
                }
                "textNodes" -> for (element in elements) {
                    val tn = arrayListOf<String>()
                    val contentEs = element.textNodes()
                    for (item in contentEs) {
                        val temp = item.text().trim { it <= ' ' }
                        if (!isEmpty(temp)) {
                            tn.add(temp)
                        }
                    }
                    textS.add(join("\n", tn))
                }
                "ownText" -> for (element in elements) {
                    textS.add(element.ownText())
                }
                "html" -> {
                    elements.select("script").remove()
                    elements.select("style").remove()
                    val html = elements.outerHtml()
                    textS.add(html)
                }
                "all" -> textS.add(elements.outerHtml())
                else -> for (element in elements) {
                    val url = element.attr(lastRule)
                    if (!isEmpty(url) && !textS.contains(url)) {
                        textS.add(url)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return textS
    }

    internal inner class SourceRule(ruleStr: String) {
        var isCss = false
        var elementsRule: String

        init {
            if (ruleStr.startsWith("@CSS:", true)) {
                isCss = true
                elementsRule = ruleStr.substring(5).trim { it <= ' ' }
            } else {
                elementsRule = ruleStr
            }
        }
    }

}
