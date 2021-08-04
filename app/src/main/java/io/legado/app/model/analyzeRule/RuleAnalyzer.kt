package io.legado.app.model.analyzeRule

//通用的规则切分处理
class RuleAnalyzer(data: String, code: Boolean = false) {

    private var queue: String = data //被处理字符串
    private var pos = 0 //当前处理到的位置
    private var start = 0 //当前处理字段的开始
    private var startX = 0 //当前规则的开始

    private var rule = ArrayList<String>()  //分割出的规则列表
    private var step: Int = 0 //分割字符的长度
    var elementsType = "" //当前分割字符串
    var innerType = true //是否为内嵌{{}}

    fun trim() { // 修剪当前规则之前的"@"或者空白符
        if (queue[pos] == '@' || queue[pos] < '!') { //在while里重复设置start和startX会拖慢执行速度，所以先来个判断是否存在需要修剪的字段，最后再一次性设置start和startX
            pos++
            while (queue[pos] == '@' || queue[pos] < '!') pos++
            start = pos //开始点推移
            startX = pos //规则起始点推移
        }
    }

    //将pos重置为0，方便复用
    fun reSetPos() {
        pos = 0
        startX = 0
    }

    /**
     * 从剩余字串中拉出一个字符串，直到但不包括匹配序列
     * @param seq 查找的字符串 **区分大小写**
     * @return 是否找到相应字段。
     */
    fun consumeTo(seq: String): Boolean {
        start = pos //将处理到的位置设置为规则起点
        val offset = queue.indexOf(seq, pos)
        return if (offset != -1) {
            pos = offset
            true
        } else false
    }

    /**
     * 从剩余字串中拉出一个字符串，直到但不包括匹配序列（匹配参数列表中一项即为匹配），或剩余字串用完。
     * @param seq 匹配字符串序列
     * @return 成功返回true并设置间隔，失败则直接返回fasle
     */
    fun consumeToAny(vararg seq: String): Boolean {

        var pos = pos //声明新变量记录匹配位置，不更改类本身的位置

        while (pos != queue.length) {

            for (s in seq) {
                if (queue.regionMatches(pos, s, 0, s.length)) {
                    step = s.length //间隔数
                    this.pos = pos //匹配成功, 同步处理位置到类
                    return true //匹配就返回 true
                }
            }

            pos++ //逐个试探
        }
        return false
    }

    /**
     * 从剩余字串中拉出一个字符串，直到但不包括匹配序列（匹配参数列表中一项即为匹配），或剩余字串用完。
     * @param seq 匹配字符序列
     * @return 返回匹配位置
     */
    private fun findToAny(vararg seq: Char): Int {

        var pos = pos //声明新变量记录匹配位置，不更改类本身的位置

        while (pos != queue.length) {

            for (s in seq) if (queue[pos] == s) return pos //匹配则返回位置

            pos++ //逐个试探

        }

        return -1
    }

    /**
     * 拉出一个非内嵌代码平衡组，存在转义文本
     */
    fun chompCodeBalanced(open: Char, close: Char): Boolean {

        var pos = pos //声明临时变量记录匹配位置，匹配成功后才同步到类的pos

        var depth = 0 //嵌套深度
        var otherDepth = 0 //其他对称符合嵌套深度

        var inSingleQuote = false //单引号
        var inDoubleQuote = false //双引号

        do {
            if (pos == queue.length) break
            val c = queue[pos++]
            if (c != ESC) { //非转义字符
                if (c == '\'' && !inDoubleQuote) inSingleQuote = !inSingleQuote //匹配具有语法功能的单引号
                else if (c == '"' && !inSingleQuote) inDoubleQuote = !inDoubleQuote //匹配具有语法功能的双引号

                if (inSingleQuote || inDoubleQuote) continue //语法单元未匹配结束，直接进入下个循环

                if (c == '[') depth++ //开始嵌套一层
                else if (c == ']') depth-- //闭合一层嵌套
                else if (depth == 0) {
                    //处于默认嵌套中的非默认字符不需要平衡，仅depth为0时默认嵌套全部闭合，此字符才进行嵌套
                    if (c == open) otherDepth++
                    else if (c == close) otherDepth--
                }

            } else pos++

        } while (depth > 0 || otherDepth > 0) //拉出一个平衡字串

        return if (depth > 0 || otherDepth > 0) false else {
            this.pos = pos //同步位置
            true
        }
    }

    /**
     * 拉出一个规则平衡组，经过仔细测试xpath和jsoup中，引号内转义字符无效。
     */
    fun chompRuleBalanced(open: Char, close: Char): Boolean {

        var pos = pos //声明临时变量记录匹配位置，匹配成功后才同步到类的pos
        var depth = 0 //嵌套深度
        var inSingleQuote = false //单引号
        var inDoubleQuote = false //双引号

        do {
            if (pos == queue.length) break
            val c = queue[pos++]
            if (c == '\'' && !inDoubleQuote) inSingleQuote = !inSingleQuote //匹配具有语法功能的单引号
            else if (c == '"' && !inSingleQuote) inDoubleQuote = !inDoubleQuote //匹配具有语法功能的双引号

            if (inSingleQuote || inDoubleQuote) continue //语法单元未匹配结束，直接进入下个循环
            else if (c == '\\') { //不在引号中的转义字符才将下个字符转义
                pos++
                continue
            }

            if (c == open) depth++ //开始嵌套一层
            else if (c == close) depth-- //闭合一层嵌套

        } while (depth > 0) //拉出一个平衡字串

        return if (depth > 0) false else {
            this.pos = pos //同步位置
            true
        }
    }

    /**
     * 不用正则,不到最后不切片也不用中间变量存储,只在序列中标记当前查找字段的开头结尾,到返回时才切片,高效快速准确切割规则
     * 解决jsonPath自带的"&&"和"||"与阅读的规则冲突,以及规则正则或字符串中包含"&&"、"||"、"%%"、"@"导致的冲突
     */
    tailrec fun splitRule(vararg split: String): ArrayList<String> { //首段匹配,elementsType为空

        if (split.size == 1) {
            elementsType = split[0] //设置分割字串
            return if (!consumeTo(elementsType)) {
                rule += queue.substring(startX)
                rule
            } else {
                step = elementsType.length //设置分隔符长度
                splitRule()
            } //递归匹配
        } else if (!consumeToAny(* split)) { //未找到分隔符
            rule += queue.substring(startX)
            return rule
        }

        val end = pos //记录分隔位置
        pos = start //重回开始，启动另一种查找

        do {
            val st = findToAny('[', '(') //查找筛选器位置

            if (st == -1) {

                rule = arrayListOf(queue.substring(startX, end)) //压入分隔的首段规则到数组

                elementsType = queue.substring(end, end + step) //设置组合类型
                pos = end + step //跳过分隔符

                while (consumeTo(elementsType)) { //循环切分规则压入数组
                    rule += queue.substring(start, pos)
                    pos += step //跳过分隔符
                }

                rule += queue.substring(pos) //将剩余字段压入数组末尾

                return rule
            }

            if (st > end) { //先匹配到st1pos，表明分隔字串不在选择器中，将选择器前分隔字串分隔的字段依次压入数组

                rule = arrayListOf(queue.substring(startX, end)) //压入分隔的首段规则到数组

                elementsType = queue.substring(end, end + step) //设置组合类型
                pos = end + step //跳过分隔符

                while (consumeTo(elementsType) && pos < st) { //循环切分规则压入数组
                    rule += queue.substring(start, pos)
                    pos += step //跳过分隔符
                }

                return if (pos > st) {
                    startX = start
                    splitRule() //首段已匹配,但当前段匹配未完成,调用二段匹配
                } else { //执行到此，证明后面再无分隔字符
                    rule += queue.substring(pos) //将剩余字段压入数组末尾
                    rule
                }
            }

            pos = st //位置推移到筛选器处
            val next = if (queue[pos] == '[') ']' else ')' //平衡组末尾字符

            if (!chompBalanced(queue[pos], next)) throw Error(
                queue.substring(0, start) + "后未平衡"
            ) //拉出一个筛选器,不平衡则报错

        } while (end > pos)

        start = pos //设置开始查找筛选器位置的起始位置

        return splitRule(* split) //递归调用首段匹配
    }

    @JvmName("splitRuleNext")
    private tailrec fun splitRule(): ArrayList<String> { //二段匹配被调用,elementsType非空(已在首段赋值),直接按elementsType查找,比首段采用的方式更快

        val end = pos //记录分隔位置
        pos = start //重回开始，启动另一种查找

        do {
            val st = findToAny('[', '(') //查找筛选器位置

            if (st == -1) {

                rule += arrayOf(queue.substring(startX, end)) //压入分隔的首段规则到数组
                pos = end + step //跳过分隔符

                while (consumeTo(elementsType)) { //循环切分规则压入数组
                    rule += queue.substring(start, pos)
                    pos += step //跳过分隔符
                }

                rule += queue.substring(pos) //将剩余字段压入数组末尾

                return rule
            }

            if (st > end) { //先匹配到st1pos，表明分隔字串不在选择器中，将选择器前分隔字串分隔的字段依次压入数组

                rule += arrayListOf(queue.substring(startX, end)) //压入分隔的首段规则到数组
                pos = end + step //跳过分隔符

                while (consumeTo(elementsType) && pos < st) { //循环切分规则压入数组
                    rule += queue.substring(start, pos)
                    pos += step //跳过分隔符
                }

                return if (pos > st) {
                    startX = start
                    splitRule() //首段已匹配,但当前段匹配未完成,调用二段匹配
                } else { //执行到此，证明后面再无分隔字符
                    rule += queue.substring(pos) //将剩余字段压入数组末尾
                    rule
                }
            }

            pos = st //位置推移到筛选器处
            val next = if (queue[pos] == '[') ']' else ')' //平衡组末尾字符

            if (!chompBalanced(queue[pos], next)) throw Error(
                queue.substring(0, start) + "后未平衡"
            ) //拉出一个筛选器,不平衡则报错

        } while (end > pos)

        start = pos //设置开始查找筛选器位置的起始位置

        return if (!consumeTo(elementsType)) {
            rule += queue.substring(startX)
            rule
        } else splitRule() //递归匹配

    }

    /**
     * 替换内嵌规则
     * @param inner 起始标志,如{$.
     * @param startStep 不属于规则部分的前置字符长度，如{$.中{不属于规则的组成部分，故startStep为1
     * @param endStep 不属于规则部分的后置字符长度
     * @param fr 查找到内嵌规则时，用于解析的函数
     *
     * */
    fun innerRule(
        inner: String,
        startStep: Int = 1,
        endStep: Int = 1,
        fr: (String) -> String?
    ): String {
        val st = StringBuilder()

        while (consumeTo(inner)) { //拉取成功返回true，ruleAnalyzes里的字符序列索引变量pos后移相应位置，否则返回false,且isEmpty为true
            val posPre = pos //记录consumeTo匹配位置
            if (chompCodeBalanced('{', '}')) {
                val frv = fr(queue.substring(posPre + startStep, pos - endStep))
                if (!frv.isNullOrEmpty()) {
                    st.append(queue.substring(startX, posPre) + frv) //压入内嵌规则前的内容，及内嵌规则解析得到的字符串
                    startX = pos //记录下次规则起点
                    continue //获取内容成功，继续选择下个内嵌规则
                }
            }
            pos += inner.length //拉出字段不平衡，inner只是个普通字串，跳到此inner后继续匹配
        }

        return if (startX == 0) "" else st.apply {
            append(queue.substring(startX))
        }.toString()
    }

    /**
     * 替换内嵌规则
     * @param fr 查找到内嵌规则时，用于解析的函数
     *
     * */
    fun innerRule(
        startStr: String,
        endStr: String,
        fr: (String) -> String?
    ): String {

        val st = StringBuilder()
        while (consumeTo(startStr)) { //拉取成功返回true，ruleAnalyzes里的字符序列索引变量pos后移相应位置，否则返回false,且isEmpty为true
            pos += startStr.length //跳过开始字符串
            val posPre = pos //记录consumeTo匹配位置
            if (consumeTo(endStr)) {
                val frv = fr(queue.substring(posPre, pos))
                st.append(
                    queue.substring(
                        startX,
                        posPre - startStr.length
                    ) + frv
                ) //压入内嵌规则前的内容，及内嵌规则解析得到的字符串
                pos += endStr.length //跳过结束字符串
                startX = pos //记录下次规则起点
            }
        }

        return if (startX == 0) queue else st.apply {
            append(queue.substring(startX))
        }.toString()
    }

    val ruleTypeList = ArrayList<String>()

    //设置平衡组函数，json或JavaScript时设置成chompCodeBalanced，否则为chompRuleBalanced
    val chompBalanced = if (code) ::chompCodeBalanced else ::chompRuleBalanced

    companion object {

        /**
         * 转义字符
         */
        private const val ESC = '\\'

    }
}