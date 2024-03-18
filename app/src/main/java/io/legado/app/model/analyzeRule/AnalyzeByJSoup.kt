package io.legado.app.model.analyzeRule

import androidx.annotation.Keep
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.jsoup.select.Collector
import org.jsoup.select.Elements
import org.jsoup.select.Evaluator
import org.seimicrawler.xpath.JXNode

/**
 * Created by GKF on 2018/1/25.
 * 书源规则解析
 */
@Keep
class AnalyzeByJSoup(doc: Any) {

    companion object {
        private val nullSet = setOf(null)
    }

    private var element: Element = parse(doc)

    private fun parse(doc: Any): Element {
        if (doc is Element) {
            return doc
        }
        if (doc is JXNode) {
            return if (doc.isElement) doc.asElement() else Jsoup.parse(doc.toString())
        }
        kotlin.runCatching {
            if (doc.toString().startsWith("<?xml", true)) {
                return Jsoup.parse(doc.toString(), Parser.xmlParser())
            }
        }
        return Jsoup.parse(doc.toString())
    }

    /**
     * 获取列表
     */
    internal fun getElements(rule: String) = getElements(element, rule)

    /**
     * 合并内容列表,得到内容
     */
    internal fun getString(ruleStr: String): String? {
        if (ruleStr.isEmpty()) {
            return null
        }
        val list = getStringList(ruleStr)
        if (list.isEmpty()) {
            return null
        }
        if (list.size == 1) {
            return list.first()
        }
        return list.joinToString("\n")
    }


    /**
     * 获取一个字符串
     */
    internal fun getString0(ruleStr: String) =
        getStringList(ruleStr).let { if (it.isEmpty()) "" else it[0] }

    /**
     * 获取所有内容列表
     */
    internal fun getStringList(ruleStr: String): List<String> {

        val textS = ArrayList<String>()

        if (ruleStr.isEmpty()) return textS

        //拆分规则
        val sourceRule = SourceRule(ruleStr)

        if (sourceRule.elementsRule.isEmpty()) {

            textS.add(element.data() ?: "")

        } else {

            val ruleAnalyzes = RuleAnalyzer(sourceRule.elementsRule)
            val ruleStrS = ruleAnalyzes.splitRule("&&", "||", "%%")

            val results = ArrayList<List<String>>()
            for (ruleStrX in ruleStrS) {

                val temp: ArrayList<String>? =
                    if (sourceRule.isCss) {
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
                    if (ruleAnalyzes.elementsType == "||") break
                }
            }
            if (results.size > 0) {
                if ("%%" == ruleAnalyzes.elementsType) {
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

        if (temp == null || rule.isEmpty()) return Elements()

        val elements = Elements()

        val sourceRule = SourceRule(rule)
        val ruleAnalyzes = RuleAnalyzer(sourceRule.elementsRule)
        val ruleStrS = ruleAnalyzes.splitRule("&&", "||", "%%")

        val elementsList = ArrayList<Elements>()
        if (sourceRule.isCss) {
            for (ruleStr in ruleStrS) {
                val tempS = temp.select(ruleStr)
                elementsList.add(tempS)
                if (tempS.size > 0 && ruleAnalyzes.elementsType == "||") {
                    break
                }
            }
        } else {
            for (ruleStr in ruleStrS) {

                val rsRule = RuleAnalyzer(ruleStr)

                rsRule.trim()  // 修剪当前规则之前的"@"或者空白符

                val rs = rsRule.splitRule("@")

                val el = if (rs.size > 1) {
                    val el = Elements()
                    el.add(temp)
                    for (rl in rs) {
                        val es = Elements()
                        for (et in el) {
                            es.addAll(getElements(et, rl))
                        }
                        el.clear()
                        el.addAll(es)
                    }
                    el
                } else ElementsSingle().getElementsSingle(temp, ruleStr)

                elementsList.add(el)
                if (el.size > 0 && ruleAnalyzes.elementsType == "||") {
                    break
                }
            }
        }
        if (elementsList.size > 0) {
            if ("%%" == ruleAnalyzes.elementsType) {
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

    /**
     * 获取内容列表
     */
    private fun getResultList(ruleStr: String): ArrayList<String>? {

        if (ruleStr.isEmpty()) return null

        var elements = Elements()

        elements.add(element)

        val rule = RuleAnalyzer(ruleStr) //创建解析

        rule.trim() //修建前置赘余符号

        val rules = rule.splitRule("@") // 切割成列表

        val last = rules.size - 1
        for (i in 0 until last) {
            val es = Elements()
            for (elt in elements) {
                es.addAll(ElementsSingle().getElementsSingle(elt, rules[i]))
            }
            elements.clear()
            elements = es
        }
        return if (elements.isEmpty()) null else getResultLast(elements, rules[last])
    }

    /**
     * 根据最后一个规则获取内容
     */
    private fun getResultLast(elements: Elements, lastRule: String): ArrayList<String> {
        val textS = ArrayList<String>()
        when (lastRule) {
            "text" -> for (element in elements) {
                val text = element.text()
                if (text.isNotEmpty()) {
                    textS.add(text)
                }
            }

            "textNodes" -> for (element in elements) {
                val tn = arrayListOf<String>()
                val contentEs = element.textNodes()
                for (item in contentEs) {
                    val text = item.text().trim { it <= ' ' }
                    if (text.isNotEmpty()) {
                        tn.add(text)
                    }
                }
                if (tn.isNotEmpty()) {
                    textS.add(tn.joinToString("\n"))
                }
            }

            "ownText" -> for (element in elements) {
                val text = element.ownText()
                if (text.isNotEmpty()) {
                    textS.add(text)
                }
            }

            "html" -> {
                elements.select("script").remove()
                elements.select("style").remove()
                val html = elements.outerHtml()
                if (html.isNotEmpty()) {
                    textS.add(html)
                }
            }

            "all" -> textS.add(elements.outerHtml())
            else -> for (element in elements) {

                val url = element.attr(lastRule)

                if (url.isBlank() || textS.contains(url)) continue

                textS.add(url)
            }
        }
        return textS
    }

    /**
     * 1.支持阅读原有写法，':'分隔索引，!或.表示筛选方式，索引可为负数
     * 例如 tag.div.-1:10:2 或 tag.div!0:3
     *
     * 2. 支持与jsonPath类似的[]索引写法
     * 格式形如 [it,it，。。。] 或 [!it,it，。。。] 其中[!开头表示筛选方式为排除，it为单个索引或区间。
     * 区间格式为 start:end 或 start:end:step，其中start为0可省略，end为-1可省略。
     * 索引，区间两端及间隔都支持负数
     * 例如 tag.div[-1, 3:-2:-10, 2]
     * 特殊用法 tag.div[-1:0] 可在任意地方让列表反向
     * */
    @Suppress("UNCHECKED_CAST")
    data class ElementsSingle(
        var split: Char = '.',
        var beforeRule: String = "",
        val indexDefault: MutableList<Int> = mutableListOf(),
        val indexes: MutableList<Any> = mutableListOf()
    ) {
        /**
         * 获取Elements按照一个规则
         */
        fun getElementsSingle(temp: Element, rule: String): Elements {

            findIndexSet(rule) //执行索引列表处理器

            /**
             * 获取所有元素
             * */
            var elements =
                if (beforeRule.isEmpty()) temp.children() //允许索引直接作为根元素，此时前置规则为空，效果与children相同
                else {
                    val rules = beforeRule.split(".")
                    when (rules[0]) {
                        "children" -> temp.children() //允许索引直接作为根元素，此时前置规则为空，效果与children相同
                        "class" -> temp.getElementsByClass(rules[1])
                        "tag" -> temp.getElementsByTag(rules[1])
                        "id" -> Collector.collect(Evaluator.Id(rules[1]), temp)
                        "text" -> temp.getElementsContainingOwnText(rules[1])
                        else -> temp.select(beforeRule)
                    }
                }

            val len = elements.size
            val lastIndexes = (indexDefault.size - 1).takeIf { it != -1 } ?: (indexes.size - 1)
            val indexSet = mutableSetOf<Int>()

            /**
             * 获取无重且不越界的索引集合
             * */
            if (indexes.isEmpty()) for (ix in lastIndexes downTo 0) { //indexes为空，表明是非[]式索引，集合是逆向遍历插入的，所以这里也逆向遍历，好还原顺序

                val it = indexDefault[ix]
                if (it in 0 until len) indexSet.add(it) //将正数不越界的索引添加到集合
                else if (it < 0 && len >= -it) indexSet.add(it + len) //将负数不越界的索引添加到集合

            } else for (ix in lastIndexes downTo 0) { //indexes不空，表明是[]式索引，集合是逆向遍历插入的，所以这里也逆向遍历，好还原顺序

                if (indexes[ix] is Triple<*, *, *>) { //区间
                    val (startX, endX, stepX) = indexes[ix] as Triple<Int?, Int?, Int> //还原储存时的类型

                    val start = if (startX == null) 0 //左端省略表示0
                    else if (startX >= 0) if (startX < len) startX else len - 1 //右端越界，设置为最大索引
                    else if (-startX <= len) len + startX /* 将负索引转正 */ else 0 //左端越界，设置为最小索引

                    val end = if (endX == null) len - 1 //右端省略表示 len - 1
                    else if (endX >= 0) if (endX < len) endX else len - 1 //右端越界，设置为最大索引
                    else if (-endX <= len) len + endX /* 将负索引转正 */ else 0 //左端越界，设置为最小索引

                    if (start == end || stepX >= len) { //两端相同，区间里只有一个数。或间隔过大，区间实际上仅有首位

                        indexSet.add(start)
                        continue

                    }

                    val step =
                        if (stepX > 0) stepX else if (-stepX < len) stepX + len else 1 //最小正数间隔为1

                    //将区间展开到集合中,允许列表反向。
                    indexSet.addAll(if (end > start) start..end step step else start downTo end step step)

                } else {//单个索引

                    val it = indexes[ix] as Int //还原储存时的类型

                    if (it in 0 until len) indexSet.add(it) //将正数不越界的索引添加到集合
                    else if (it < 0 && len >= -it) indexSet.add(it + len) //将负数不越界的索引添加到集合

                }

            }

            /**
             * 根据索引集合筛选元素
             * */
            if (split == '!') { //排除

                for (pcInt in indexSet) elements[pcInt] = null

                elements.removeAll(nullSet) //测试过，这样就行

            } else if (split == '.') { //选择

                val es = Elements()

                for (pcInt in indexSet) es.add(elements[pcInt])

                elements = es

            }

            return elements //返回筛选结果

        }

        private fun findIndexSet(rule: String) {

            val rus = rule.trim { it <= ' ' }

            var len = rus.length
            var curInt: Int? //当前数字
            var curMinus = false //当前数字是否为负
            val curList = mutableListOf<Int?>() //当前数字区间
            var l = "" //暂存数字字符串

            val head = rus.last() == ']' //是否为常规索引写法

            if (head) { //常规索引写法[index...]

                len-- //跳过尾部']'

                while (len-- >= 0) { //逆向遍历,可以无前置规则

                    var rl = rus[len]
                    if (rl == ' ') continue //跳过空格

                    if (rl in '0'..'9') l = rl + l //将数值累接入临时字串中，遇到分界符才取出
                    else if (rl == '-') curMinus = true
                    else {

                        curInt =
                            if (l.isEmpty()) null else if (curMinus) -l.toInt() else l.toInt() //当前数字

                        when (rl) {

                            ':' -> curList.add(curInt) //区间右端或区间间隔

                            else -> {

                                //为保证查找顺序，区间和单个索引都添加到同一集合
                                if (curList.isEmpty()) {

                                    if (curInt == null) break //是jsoup选择器而非索引列表，跳出

                                    indexes.add(curInt)
                                } else {

                                    //列表最后压入的是区间右端，若列表有两位则最先压入的是间隔
                                    indexes.add(
                                        Triple(
                                            curInt,
                                            curList.last(),
                                            if (curList.size == 2) curList.first() else 1
                                        )
                                    )

                                    curList.clear() //重置临时列表，避免影响到下个区间的处理

                                }

                                if (rl == '!') {
                                    split = '!'
                                    do {
                                        rl = rus[--len]
                                    } while (len > 0 && rl == ' ')//跳过所有空格
                                }

                                if (rl == '[') {
                                    beforeRule = rus.substring(0, len) //遇到索引边界，返回结果
                                    return
                                }

                                if (rl != ',') break //非索引结构，跳出

                            }
                        }

                        l = "" //清空
                        curMinus = false //重置
                    }
                }
            } else while (len-- >= 0) { //阅读原本写法，逆向遍历,可以无前置规则

                val rl = rus[len]
                if (rl == ' ') continue //跳过空格

                if (rl in '0'..'9') l = rl + l //将数值累接入临时字串中，遇到分界符才取出
                else if (rl == '-') curMinus = true
                else {

                    if (rl == '!' || rl == '.' || rl == ':') { //分隔符或起始符

                        indexDefault.add(if (curMinus) -l.toInt() else l.toInt()) // 当前数字追加到列表

                        if (rl != ':') { //rl == '!'  || rl == '.'
                            split = rl
                            beforeRule = rus.substring(0, len)
                            return
                        }

                    } else break //非索引结构，跳出循环

                    l = "" //清空
                    curMinus = false //重置
                }
            }

            split = ' '
            beforeRule = rus
        }
    }


    internal inner class SourceRule(ruleStr: String) {
        var isCss = false
        var elementsRule: String = if (ruleStr.startsWith("@CSS:", true)) {
            isCss = true
            ruleStr.substring(5).trim { it <= ' ' }
        } else {
            ruleStr
        }
    }

}
