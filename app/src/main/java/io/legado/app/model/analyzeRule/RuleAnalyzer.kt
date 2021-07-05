package io.legado.app.model.analyzeRule

//通用的规则切分处理

class RuleAnalyzer(data: String) {

    private var queue: String = data //被处理字符串
    private var pos = 0 //处理到的位置
    private var rule  = arrayOf<String>()

    private var start = 0 //每次处理字段的开始
    private var startX = 0 //规则的开始
    private var step:Int = 0 //分割字符的长度

    var elementsType = ""

    //当前平衡字段
    fun currBalancedString( stepStart:Int = 1 , stepEnd:Int = 1): String { //stepStart平衡字符的起始分隔字串长度，stepEnd平衡字符的结束分隔字串长度
        return queue.substring(start+stepStart,pos-stepEnd) //当前平衡字段
    }


    fun trim(){ // 修剪当前规则之前的"@"或者空白符
        while (queue[pos] == '@' || queue[pos] < '!') pos++
    }

    //将pos重置为0，方便复用
    fun reSetPos() {
        pos = 0
    }

    //当前拉取字段
    fun currString(): String {
        return queue.substring(start,pos) //当前拉取到的字段
    }

    //剩余字串
    private fun remainingString(): String {
        start = pos
        pos = queue.length
        return queue.substring(start)
    }

    /**
     * 是否已无剩余字符?
     * @return 若剩余字串中已无字符则返回true
     */
    val isEmpty: Boolean
        get() = queue.length - pos  == 0 //是否处理到最后

    /**
     * 检索并返回首字符,但pos不变
     * @return 首字符：若为空则为 0 号字符
     */
    fun peek(): Char { //检索首字符
        return if (isEmpty) 0.toChar() else queue[pos]
    }

    /**
     * 消耗剩余字串中一个字符。
     * @return 返回剩余字串中的下个字符。
     */
    fun consume(): Char {
        return queue[pos++]
    }

    /**
     * 字串与剩余字串是否匹配，不区分大小写
     * @param seq 字符串被检查
     * @return 若下字符串匹配返回 true
     */
    fun matches(seq: String): Boolean {
        return queue.regionMatches(pos, seq, 0, seq.length, ignoreCase = true)
    }

    /**
     * 从剩余字串中拉出一个字符串，直到但不包括匹配序列，或剩余字串用完。
     * @param seq 分隔字符 **区分大小写**
     * @return 是否找到相应字段。
     */
    fun consumeTo(seq: String,setStartPos:Boolean = true): Boolean {

        if(setStartPos)start = pos //将处理到的位置设置为规则起点
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
    fun consumeToAny(vararg seq:String): Boolean {

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
    private fun findToAny(vararg seq:Char): Int {

        var pos = pos //声明新变量记录匹配位置，不更改类本身的位置

        while (pos != queue.length) {

            for (s in seq) if(queue[pos] == s) return pos //匹配则返回位置

            pos++ //逐个试探

        }

        return -1
    }

    //其中js只要符合语法，就不用避开任何阅读关键字，自由发挥
    fun chompJsBalanced(f: ((Char) -> Boolean?) = {
        when (it) {
            '{' -> true //开始嵌套一层
            '}' -> false //闭合一层嵌套
            else -> null
        }
    } ): Boolean {
        var pos = pos //声明变量记录临时处理位置
        var depth = 0 //嵌套深度
        var bracketsDepth = 0 //[]嵌套深度

        var inSingleQuote = false //单引号
        var inDoubleQuote = false //双引号
        var inOtherQuote = false //js原始字串分隔字符
        var regex = false //正则
        var commit = false //单行注释
        var commits = false //多行注释

        do {
            if (pos == queue.length) break
            var c = queue[pos++]
            if (c != '\\') { //非转义字符
                if (c == '\'' && !commits && !commit && !regex && !inDoubleQuote && !inOtherQuote) inSingleQuote = !inSingleQuote //匹配具有语法功能的单引号
                else if (c == '"' && !commits && !commit && !regex && !inSingleQuote && !inOtherQuote) inDoubleQuote = !inDoubleQuote //匹配具有语法功能的双引号
                else if (c == '`' && !commits && !commit && !regex && !inSingleQuote && !inDoubleQuote) inOtherQuote = !inOtherQuote //匹配具有语法功能的'`'
                else if (c == '/' && !commits && !commit && !regex && !inSingleQuote && !inDoubleQuote && !inOtherQuote) { //匹配注释或正则起点
                    c = queue[pos++]
                    when(c){
                        '/'->commit=true //匹配单行注释起点
                        '*'->commits=true //匹配多行注释起点
                        else ->regex=true //匹配正则起点
                    }
                }
                else if(commits && c == '*') { //匹配多行注释终点
                    c = queue[pos++]
                    if(c == '/')commits = false
                }
                else if(regex && c == '/') { //正则的终点或[]平衡

                    when (c) {
                        '/' -> regex = false//匹配正则终点

                        //为了保证当open为（ 且 close 为 ）时，正则中[(]或[)]的合法性。故对[]这对在任何规则中都平衡的成对符号做匹配。
                        // 注：正则里[(]、[)]、[{]、[}]都是合法的，所以只有[]必须平衡。
                        '[' -> bracketsDepth++ //开始嵌套一层[]
                        ']' -> bracketsDepth-- //闭合一层嵌套[]
                    }

                }else if(c == '\n') commit = false

                if (commits || commit || regex || inSingleQuote  || inDoubleQuote || inOtherQuote) continue //语法单元未匹配结束，直接进入下个循环

                val fn = f(c) ?: continue
                if (fn) depth++ else depth-- //嵌套或者闭合

            }else pos++
//            { //转义字符
//                var next = queue[pos++] //拉出被转义字符
//                if(commit && next == 'n') commit = false //匹配单行注释终点。当前为\,下个为n，表示换行
//                else if (!commits && !commit && next == '\\') {
//                    queue[pos++] //当前为\,下个为\，双重转义中"\\"表示转义字符本身，根据if条件"\\"字串不在注释中，则只能在字串或正则中
//                    next = queue[pos++] //拉出下个字符，因为在双重转义的字串或正则中，类似于 \\/ 这样的结构才是转义结构
//                    if(next == '\\')queue[pos++] //若为转义字符则继续拉出，因为双重转义中转义字符成对存在,即 \\\\
//                }
//            }
        } while (depth > 0 || bracketsDepth >0) //拉出全部符合js语法的字段

        return if(depth > 0 || bracketsDepth > 0) false else {
            this.pos = pos //同步位置
            true
        }
    }

    /**
     * 在双重转义字串中拉出一个规则平衡组
     */
    fun chompRuleBalanced(open: Char = '[', close: Char = ']',f: ((Char) ->Boolean?)? = null ): Boolean {

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

                if (inSingleQuote  || inDoubleQuote) continue //语法单元未匹配结束，直接进入下个循环

                if ( c == open )depth++ //开始嵌套一层
                else if ( c== close) depth-- //闭合一层嵌套
                else if(depth == 0 && f != null) { //处于默认嵌套中的非默认字符不需要平衡，仅depth为0时默认嵌套全部闭合，此字符才进行嵌套
                    val fn = f(c) ?: continue
                    if (fn) otherDepth++ else otherDepth--
                }

            }else pos++
//            { //转义字符
//                var next = queue[pos++] //拉出被转义字符，匹配\/、\"、\'等
//                if (next == ESC) {
//                    queue[pos++] //当前为\,下个为\，双重转义中"\\"表示转义字符本身，根据语法特征当前字段在字串或正则中
//                    next = queue[pos++] //拉出下个字符，因为在双重转义的字串或正则中，类似于 \\/ 这样的结构才是转义结构
//                    if(next == ESC)queue[pos++] //若为转义字符则继续拉出，因为双重转义中转义字符成对存在,即 \\\\
//                }
//            }
        } while (depth > 0 || otherDepth > 0) //拉出一个平衡字串

        return if(depth > 0 || otherDepth > 0) false else {
            this.pos = pos //同步位置
            true
        }
    }

    /**
     * 不用正则,不到最后不切片也不用中间变量存储,只在序列中标记当前查找字段的开头结尾,到返回时才切片,高效快速准确切割规则
     * 解决jsonPath自带的"&&"和"||"与阅读的规则冲突,以及规则正则或字符串中包含"&&"、"||"、"%%"、"@"导致的冲突
     */
    tailrec fun splitRule(vararg split: String): Array<String>{ //首段匹配,elementsType为空

        if(split.size == 1) {
            elementsType = split[0] //设置分割字串
            return if(!consumeTo(elementsType)) {
                rule += queue.substring(startX)
                rule
            }else {
                step = elementsType.length //设置分隔符长度
                splitRule()
            } //递归匹配
        }else if (!consumeToAny(* split)) { //未找到分隔符
            rule += queue.substring(startX)
            return rule
        }

        val end = pos //记录分隔位置
        pos = start //重回开始，启动另一种查找

        do{
            val st = findToAny('[', '(') //查找筛选器位置

            if (st == -1) {

                rule = arrayOf(queue.substring(startX, end)) //压入分隔的首段规则到数组

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

                rule = arrayOf(queue.substring(startX, end)) //压入分隔的首段规则到数组

                elementsType = queue.substring(end, end + step) //设置组合类型
                pos = end + step //跳过分隔符

                while (consumeTo(elementsType) && pos < st) { //循环切分规则压入数组
                    rule += queue.substring(start, pos)
                    pos += step //跳过分隔符
                }

                return if(pos > st) {
                    startX = start
                    splitRule() //首段已匹配,但当前段匹配未完成,调用二段匹配
                }
                else { //执行到此，证明后面再无分隔字符
                    rule += queue.substring(pos) //将剩余字段压入数组末尾
                    rule
                }
            }

            pos = st //位置推移到筛选器处
            val next = if (queue[pos] == '[') ']' else ')' //平衡组末尾字符

            if (!chompRuleBalanced(queue[pos], next)) throw Error(
                queue.substring(
                    0,
                    start
                ) + "后未平衡"
            ) //拉出一个筛选器,不平衡则报错

        }while( end > pos )

        start = pos //设置开始查找筛选器位置的起始位置

        return splitRule(* split) //递归调用首段匹配
    }

    @JvmName("splitRuleNext")
    private tailrec fun splitRule(): Array<String>{ //二段匹配被调用,elementsType非空(已在首段赋值),直接按elementsType查找,比首段采用的方式更快

        val end = pos //记录分隔位置
        pos = start //重回开始，启动另一种查找

        do{
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

                rule += arrayOf(queue.substring(startX, end)) //压入分隔的首段规则到数组
                pos = end + step //跳过分隔符

                while (consumeTo(elementsType) && pos < st) { //循环切分规则压入数组
                    rule += queue.substring(start, pos)
                    pos += step //跳过分隔符
                }

                return if(pos > st) {
                    startX = start
                    splitRule() //首段已匹配,但当前段匹配未完成,调用二段匹配
                }
                else { //执行到此，证明后面再无分隔字符
                    rule += queue.substring(pos) //将剩余字段压入数组末尾
                    rule
                }
            }

            pos = st //位置推移到筛选器处
            val next = if (queue[pos] == '[') ']' else ')' //平衡组末尾字符

            if (!chompRuleBalanced(queue[pos], next)) throw Error(
                queue.substring(
                    0,
                    start
                ) + "后未平衡"
            ) //拉出一个筛选器,不平衡则报错

        }while( end > pos )

        start = pos //设置开始查找筛选器位置的起始位置

        return if(!consumeTo(elementsType)) {
            rule += queue.substring(startX)
            rule
        }else splitRule() //递归匹配

    }


    /**
     * 替换内嵌规则
     * @param inner 起始标志,如{$. 或 {{
     * @param startStep 不属于规则部分的前置字符长度，如{$.中{不属于规则的组成部分，故startStep为1
     * @param endStep 不属于规则部分的后置字符长度，如}}长度为2
     * @param fr 查找到内嵌规则时，用于解析的函数
     *
     * */
    fun innerRule( inner:String,startStep:Int = 1,endStep:Int = 1,fr:(String)->String?): String {

        val start0 = pos //规则匹配前起点

        val st = StringBuilder()

        while (!isEmpty && consumeTo(inner)) { //拉取成功返回true，ruleAnalyzes里的字符序列索引变量pos后移相应位置，否则返回false,且isEmpty为true
            val posPre = pos //记录上次结束位置
            if (chompRuleBalanced {//拉出一个以[]为默认嵌套、以{}为补充嵌套的平衡字段
                    when (it) {
                        '{' -> true
                        '}' -> false
                        else -> null
                    }
                }) {
                val frv= fr(currBalancedString(startStep,endStep))
                if(frv != null) {

                    st.append(queue.substring(start,posPre)+frv) //压入内嵌规则前的内容，及内嵌规则解析得到的字符串
                    continue //获取内容成功，继续选择下个内嵌规则

                }
            }

            pos += inner.length //拉出字段不平衡，inner只是个普通字串，跳到此inner后继续匹配

        }

        //匹配前起点与当前规则起点相同，证明无替换成功的内嵌规则,返回空字符串。否则返回替换后的字符串
        return if(start0 == start) "" else {
            st.append(remainingString()) //压入剩余字符串
            st.toString()
        }
    }

//    /**
//     * 匹配并返回标签中的属性键字串（字母、数字、-、_、:）
//     * @return 属性键字串
//     */
//    fun consumeAttributeKey(start:Int = pos): String {
//        while (!isEmpty && (Character.isLetterOrDigit(queue[pos]) || matchesAny('-', '_', ':'))) pos++
//        return queue.substring(start, pos)
//    }

//    fun splitRule(query:String,item:String = "other",listItem:String = "allInOne"):String{
//
//        val cuurItem = item //当前项类型，list->列表项 mulu->章节列表项 url->链接项 search->搜索链接项 find发现链接列表项 other->其他项
//        val cuurList = listItem//当前界面总列表项类型，allInOne，json，xml，kotin，java
//        var Reverse = false //是否反转列表
//
//        consumeWhitespace() //消耗开头空白
//        var fisrt = consume() //拉出并消费首字符
//
//        when(item){
//            "search" ->
//            "find" ->
//            "mulu" -> if(fisrt == '-'){
//                Reverse=true //开启反转
//                consumeWhitespace() //拉出所有空白符
//                fisrt = consume() //首字符后移
//            }
//            else ->
//
//        }
//
//        return  query
//    }

    companion object {
        /**
         * 转义字符
         */
        private const val ESC = '\\'

        /**
         * 阅读共有分隔字串起始部分
         * "##","@@","{{","{[","<js>", "@js:"
         */
        val splitList =arrayOf("##","@@","{{","{[","<js>", "@js:")

        /**
         * 发现‘名称-链接’分隔字串
         * "::"
         */
        const val splitListFaXian = "::"

        /**
         * 目录专有起始字符
         * "-"
         */
        const val splitListMulu = "-"

        /**
         * 结果为元素列表的 all in one 模式起始字符
         * "+"
         */
        const val splitListTongYi = "+"

        /**
         * 结果为元素列表的项的同规则组合结构
         * "||","&&","%%"
         */
        val splitListReSplit = arrayOf("||","&&","%%")

        /**
         * js脚本结束字串
         * "</js>"
         */
        const val splitListEndJS = "</js>"

        /**
         *内嵌js结束字串
         * "}}"
         */
        const val splitListEndInnerJS = "}}"

        /**
         * 内嵌规则结束字串
         * "]}"
         */
        const val splitListEndInnerRule = "]}"

        /**
         * '[', ']', '(', ')','{','}'
         */
        val splitListPublic = charArrayOf('[', ']', '(', ')','{','}')

        /**
         * '*',"/","//",":","::","@","|","@xpath:"
         */
        val splitListXpath = arrayOf("*","/","//",":","::","@","|","@xpath:")

        /**
         * '*','$',".","..", "@json:"
         */
        val splitListJson = arrayOf('*','$',".","..", "@json:")

        /**
         * '*',"+","~",".",",","|","@","@css:",":"
         */
        val splitListCss = arrayOf('*',"+","~",".",",","|","@","@css:",":")

        /**
         * "-",".","!","@","@@"
         */
        val splitListDefault = arrayOf("-",".","!","@","@@")

    }
}
