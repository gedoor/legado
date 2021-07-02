package io.legado.app.model.analyzeRule

import android.text.TextUtils.join
import androidx.annotation.Keep
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
        /**
         * "class", "id", "tag", "text", "children"
         */
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
    internal fun getElements(rule: String) = getElements(element, rule)

    /**
     * 合并内容列表,得到内容
     */
    internal fun getString(ruleStr: String) =
        if(ruleStr.isEmpty()) null
        else getStringList(ruleStr).takeIf { it.isNotEmpty() }?.joinToString("\n")

    /**
     * 获取一个字符串
     */
    internal fun getString0(ruleStr: String) = getStringList(ruleStr).let{ if ( it.isEmpty() ) "" else it[0] }

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
            val ruleStrS = ruleAnalyzes.splitRule("&&","||" ,"%%")

            val results = ArrayList<List<String>>()
            for (ruleStrX in ruleStrS) {

                val temp: List<String>? =
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

                    results.add(temp) //!temp.isNullOrEmpty()时，results.isNotEmpty()为true

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
        val ruleStrS = ruleAnalyzes.splitRule("&&","||","%%")

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
                //将原getElementsSingle函数调用的函数的部分代码内联过来，方便简化getElementsSingle函数

                val rsRule = RuleAnalyzer(ruleStr)

                if( rsRule.peek() =='@' || rsRule.peek() < '!' ) rsRule.advance()  // 修剪当前规则之前的"@"或者空白符

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
                }else getElementsSingle(temp,ruleStr)

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
     * '.'开头表示选择元素,或'!'开头排除那些元素。两者都支持以索引列表按顺序指定元素列表
     * ':'分隔不同索引或区间
     * 区间格式为 start~end+step，其中start为0可省略，end为-1可省略。
     * 索引，区间两端及间隔都支持负数
     * 例如 tag.div.-1:3~-2+-10:2
     * 特殊用法 tag.div.-1~0 可在任意地方让列表反向
     * */
    fun findIndexSet( rule:String ): IndexSet {

        val indexSet = IndexSet()

        val rus = rule.trim{ it <= ' '}

        var last = rus.length
        var step = 0 //区间步长，为0表示没设置区间
        var curInt: Int //当前数字
        var end = 0 //暂存区间结束数字

        var range = false //true表示当前在区间开头,false表示当前在区间结尾
        var curMinus = false //当前数字是否为负
        var curEndMinus = false //当前区间右端数字是否为负
        var curStepMinus = false //当前区间间隔数字是否为负

        var l = "" //暂存数字字符串

        while (last --> 1 ){ //逆向遍历,至少有两位前置字符,如 p.

            val rl = rus[last]
            if(rl == ' ' )continue //跳过空格
            if( rl in '0'..'9') l+= rl //将数值累接入临时字串中，遇到分界符才取出
            else if(rl == '-') curMinus = true
            else if( rl in arrayOf('+','~','!','.',':')) { //分界符号 '+','~','!','.',':'

                when ( rl ) {

                    '+' ->{
                        curStepMinus = curMinus
                        step = l.toInt() //区间间隔数
                    }

                    '~' -> {
                        range = true
                        curEndMinus = curMinus

                        if (l.isEmpty()) {
                            end = -1 //省略区间右端，设置为-1
                            continue
                        } else end = l.toInt()
                    }

                    else -> {

                        curInt =  if(l.isEmpty()) 0 /* 省略区间左端，设置为0 */ else if(curMinus) - l.toInt() else l.toInt() //区间左端数,省略则为最左边

                        indexSet.indexs.add( //压入以下值，为保证查找顺序，区间和单个索引都添加到同一集合

                            if ( range ) {

                                range = false //重置

                                if (curEndMinus) {
                                    end = -end
                                    curEndMinus = false //重置
                                }

                                //没设置间隔时，间隔为1。将区间的三项数据压入，在获取到元素数量后再计算负数索引，并展开区间
                                if( step == 0 ) Triple(curInt, end, 1)

                                else {

                                    if (curStepMinus) {
                                        step = -step
                                        curStepMinus = false //重置
                                    }

                                    val stepx = step
                                    step = 0 //重置

                                    //将区间的三项数据压入，在获取到元素数量后再计算负数索引，并展开区间
                                    Triple(curInt, end, stepx)

                                }

                            }else curInt //压入单个索引，在获取到元素数量后再计算负数索引

                        )

                        if( rl == '!' || rl == '.' ) return indexSet.apply{
                            split = rl
                            beforeRule = rus.substring(0, last)
                        }
                    }
                }
                l = "" //清空
                curMinus = false //重置
            }

            else  break

        }

        return indexSet.apply{ beforeRule = rus } //非索引格式
    }

    /**
     * 获取Elements按照一个规则
     */
    private fun getElementsSingle(temp: Element, rule: String): Elements {

        var elements = Elements()

        val fi = findIndexSet(rule) //执行索引列表处理器

        val (filterType,ruleStr) = fi //获取操作类型及非索引部分的规则字串

//        val rulePc = rulePcx[0].trim { it <= ' ' }.split(">")
//        jsoup中，当前节点是参与选择的，tag.div 与 tag.div@tag.div 结果相同
//        此处">"效果和“@”完全相同，且容易让人误解成选择子节点，实际并不是。以后不允许这种无意义的写法

        val rules = ruleStr.split(".")

        elements.addAll(
            when (rules[0]) {
                "children" -> temp.children()
                "class" -> temp.getElementsByClass(rules[1])
                "tag" -> temp.getElementsByTag(rules[1])
                "id" -> Collector.collect(Evaluator.Id(rules[1]), temp)
                "text" -> temp.getElementsContainingOwnText(rules[1])
                else -> temp.select(ruleStr)
            } )

        val indexSet = fi.getIndexs(elements.size) //传入元素数量，处理负数索引及索引越界问题，生成可用索引集合。

        if(filterType == '!'){ //排除

            for (pcInt in indexSet) elements[pcInt] = null

            elements.removeAll( Elements().apply { add(null) } )

        }else if(filterType == '.'){ //选择

            val es = Elements()

            for (pcInt in indexSet) es.add(elements[pcInt])

            elements = es

        }

        return elements
    }

    /**
     * 获取内容列表
     */
    private fun getResultList(ruleStr: String): List<String>? {

        if (ruleStr.isEmpty()) return null

        var elements = Elements()

        elements.add(element)

        val rule = RuleAnalyzer(ruleStr) //创建解析

        while( rule.peek() =='@' || rule.peek() < '!' ) rule.advance()  // 修剪当前规则之前的"@"或者空白符

        val rules = rule.splitRule("@") // 切割成列表

        val last = rules.size - 1
        for (i in 0 until last) {
            val es = Elements()
            for (elt in elements) {
                es.addAll(getElementsSingle(elt, rules[i]))
            }
            elements.clear()
            elements = es
        }
        return if (elements.isEmpty()) null else getResultLast(elements, rules[last])
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
                        if (temp.isNotEmpty()) {
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

                    if(url.isEmpty() || textS.contains(url)) break

                    textS.add(url)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return textS
    }

    data class IndexSet(var split:Char = ' ',
                        var beforeRule:String = "",
                        val indexs:MutableList<Any> = mutableListOf()){

        fun getIndexs(len:Int): MutableSet<Int> {

            val indexSet = mutableSetOf<Int>()

            val lastIndexs = indexs.size - 1

            for (ix in lastIndexs downTo 0 ){ //逆向遍历，还原顺序

                if(indexs[ix] is Triple<*, *, *>){ //区间

                    var (start, end, step) = indexs[ix] as Triple<Int, Int, Int> //还原储存时的类型

                    if (start >= 0) {
                        if (start >= len) start = len - 1 //右端越界，设置为最大索引
                    } else start = if (-start <= len) len + start /* 将负索引转正 */ else 0 //左端越界，设置为最小索引

                    if (end >= 0) {
                        if (end >= len) end = len - 1 //右端越界，设置为最大索引
                    } else end = if (-end <= len) len + end /* 将负索引转正 */ else 0 //左端越界，设置为最小索引

                    if (start == end || step >= len) { //两端相同，区间里只有一个数。或间隔过大，区间实际上仅有首位

                        indexSet.add(start)
                        continue

                    }

                    step = if (step > 0) step else if (-step < len) step + len else 1 //最小正数间隔为1

                    //将区间展开到集合中,允许列表反向。
                    indexSet.addAll(if (end > start) start..end step step else start downTo end step step)

                }else{//单个索引

                    val it = indexs[ix] as Int //还原储存时的类型

                    if(it in 0 until len) indexSet.add(it) //将正数不越界的索引添加到集合
                    else if(it < 0 && len >= -it) indexSet.add(it + len) //将负数不越界的索引添加到集合

                }

            }

            return indexSet

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
