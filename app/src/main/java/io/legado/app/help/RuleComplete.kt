package io.legado.app.help


@Suppress("RegExpRedundantEscape")
object RuleComplete {
    // 需要补全
    private val needComplete = Regex(
        """(?<!(@|/|^|[|%&]{2})(attr|text|ownText|textNodes|href|content|html|alt|all|value|src)(\(\))?)(?<seq>\&{2}|%%|\|{2}|$)"""
    )

    // 不能补全 存在js/json/{{xx}}的复杂情况
    private val notComplete = Regex("""^:|^##|\{\{|@js:|<js>|@Json:|\$\.""")

    // 修正从图片获取信息
    private val fixImgInfo =
        Regex("""(?<=(^|tag\.|[\+/@>~| &]))img(?<at>(\[@?.+\]|\.[-\w]+)?)[@/]+text(\(\))?(?<seq>\&{2}|%%|\|{2}|$)""")

    private val isXpath = Regex("^//|^@Xpath:")

    /**
     * 对简单规则进行补全，简化部分书源规则的编写
     * 对JSOUP/XPath/CSS规则生效
     * @author 希弥
     * @return 补全后的规则 或 原规则
     * @param rules 需要补全的规则
     * @param preRule 预处理规则或列表规则
     * @param type 补全结果的类型，可选的值有:
     *  1 文字(默认)
     *  2 链接
     *  3 图片
     */
    fun autoComplete(
        rules: String?,
        preRule: String? = null,
        type: Int = 1
    ): String? {
        if (rules.isNullOrEmpty() || rules.contains(notComplete) || preRule?.contains(notComplete) == true) {
            return rules
        }

        /** 尾部##分割的正则或由,分割的参数 */
        val tailStr: String

        /** 分割字符 */
        val splitStr: String

        /**  用于获取文字时添加的规则 */
        val textRule: String

        /**  用于获取链接时添加的规则 */
        val linkRule: String

        /**  用于获取图片时添加的规则 */
        val imgRule: String

        /**  用于获取图片alt属性时添加的规则 */
        val imgText: String

        // 分离尾部规则
        val regexSplit = rules.split("""##|,\{""".toRegex(), 2)
        val cleanedRule = regexSplit[0]
        if (regexSplit.size > 1) {
            splitStr = """##|,\{""".toRegex().find(rules)?.value ?: ""
            tailStr = splitStr + regexSplit[1]
        } else {
            tailStr = ""
        }
        if (cleanedRule.contains(isXpath)) {
            textRule = "//text()\${seq}"
            linkRule = "//@href\${seq}"
            imgRule = "//@src\${seq}"
            imgText = "img\${at}/@alt\${seq}"
        } else {
            textRule = "@text\${seq}"
            linkRule = "@href\${seq}"
            imgRule = "@src\${seq}"
            imgText = "img\${at}@alt\${seq}"
        }
        return when (type) {
            1 -> needComplete.replace(cleanedRule, textRule).replace(fixImgInfo, imgText) + tailStr
            2 -> needComplete.replace(cleanedRule, linkRule) + tailStr
            3 -> needComplete.replace(cleanedRule, imgRule) + tailStr
            else -> rules
        }
    }


}
