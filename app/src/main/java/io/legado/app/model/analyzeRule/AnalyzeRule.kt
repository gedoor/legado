package io.legado.app.model.analyzeRule

import android.text.TextUtils
import androidx.annotation.Keep
import io.legado.app.constant.AppConst.SCRIPT_ENGINE
import io.legado.app.constant.AppPattern.JS_PATTERN
import io.legado.app.data.entities.BaseBook
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.CacheManager
import io.legado.app.help.JsExtensions
import io.legado.app.help.http.CookieStore
import io.legado.app.utils.*
import kotlinx.coroutines.runBlocking
import org.jsoup.nodes.Entities
import org.mozilla.javascript.NativeObject
import java.net.URL
import java.util.*
import java.util.regex.Pattern
import javax.script.SimpleBindings
import kotlin.collections.HashMap

/**
 * 解析规则获取结果
 */
@Keep
@Suppress("unused", "RegExpRedundantEscape")
class AnalyzeRule(val ruleData: RuleDataInterface) : JsExtensions {

    var book = if (ruleData is BaseBook) ruleData else null

    var chapter: BookChapter? = null
    var nextChapterUrl: String? = null
    var content: Any? = null
    var baseUrl: String? = null
    var redirectUrl: URL? = null
    private var isJSON: Boolean = false
    private var isRegex: Boolean = false

    private var analyzeByXPath: AnalyzeByXPath? = null
    private var analyzeByJSoup: AnalyzeByJSoup? = null
    private var analyzeByJSonPath: AnalyzeByJSonPath? = null

    private var objectChangedXP = false
    private var objectChangedJS = false
    private var objectChangedJP = false

    @JvmOverloads
    fun setContent(content: Any?, baseUrl: String? = null): AnalyzeRule {
        if (content == null) throw AssertionError("内容不可空（Content cannot be null）")
        this.content = content
        isJSON = content.toString().isJson()
        setBaseUrl(baseUrl)
        objectChangedXP = true
        objectChangedJS = true
        objectChangedJP = true
        return this
    }

    fun setBaseUrl(baseUrl: String?): AnalyzeRule {
        baseUrl?.let {
            this.baseUrl = baseUrl
        }
        return this
    }

    fun setRedirectUrl(url: String): URL? {
        kotlin.runCatching {
            val urlMatcher = AnalyzeUrl.paramPattern.matcher(url)
            redirectUrl = URL(if (urlMatcher.find()) url.substring(0, urlMatcher.start()) else url)
        }
        return redirectUrl
    }

    /**
     * 获取XPath解析类
     */
    private fun getAnalyzeByXPath(o: Any): AnalyzeByXPath {
        return if (o != content) {
            AnalyzeByXPath(o)
        } else {
            if (analyzeByXPath == null || objectChangedXP) {
                analyzeByXPath = AnalyzeByXPath(content!!)
                objectChangedXP = false
            }
            analyzeByXPath!!
        }
    }

    /**
     * 获取JSOUP解析类
     */
    private fun getAnalyzeByJSoup(o: Any): AnalyzeByJSoup {
        return if (o != content) {
            AnalyzeByJSoup(o)
        } else {
            if (analyzeByJSoup == null || objectChangedJS) {
                analyzeByJSoup = AnalyzeByJSoup(content!!)
                objectChangedJS = false
            }
            analyzeByJSoup!!
        }
    }

    /**
     * 获取JSON解析类
     */
    private fun getAnalyzeByJSonPath(o: Any): AnalyzeByJSonPath {
        return if (o != content) {
            AnalyzeByJSonPath(o)
        } else {
            if (analyzeByJSonPath == null || objectChangedJP) {
                analyzeByJSonPath = AnalyzeByJSonPath(content!!)
                objectChangedJP = false
            }
            analyzeByJSonPath!!
        }
    }

    /**
     * 获取文本列表
     */
    @JvmOverloads
    fun getStringList(rule: String?, isUrl: Boolean = false): List<String>? {
        if (rule.isNullOrEmpty()) return null
        val ruleList = splitSourceRule(rule, true)
        return getStringList(ruleList, isUrl)
    }

    @JvmOverloads
    fun getStringList(ruleList: List<SourceRule>, isUrl: Boolean = false): List<String>? {
        var result: Any? = null
        val content = this.content
        if (content != null && ruleList.isNotEmpty()) {
            result = content
            if (content is NativeObject) {
                result = content[ruleList[0].rule]?.toString()
            } else {
                for (sourceRule in ruleList) {
                    putRule(sourceRule.putMap)
                    sourceRule.makeUpRule(result)
                    result?.let {
                        if (sourceRule.rule.isNotEmpty()) {
                            result = when (sourceRule.mode) {
                                Mode.Js -> evalJS(sourceRule.rule, result)
                                Mode.Json -> getAnalyzeByJSonPath(it).getStringList(sourceRule.rule)
                                Mode.XPath -> getAnalyzeByXPath(it).getStringList(sourceRule.rule)
                                Mode.Default -> getAnalyzeByJSoup(it).getStringList(sourceRule.rule)
                                else -> sourceRule.rule
                            }
                        }
                        if (sourceRule.replaceRegex.isNotEmpty() && result is List<*>) {
                            val newList = ArrayList<String>()
                            for (item in result as List<*>) {
                                newList.add(replaceRegex(item.toString(), sourceRule))
                            }
                            result = newList
                        } else if (sourceRule.replaceRegex.isNotEmpty()) {
                            result = replaceRegex(result.toString(), sourceRule)
                        }
                    }
                }
            }
        }
        if (result == null) return null
        if (result is String) {
            result = (result as String).split("\n")
        }
        if (isUrl) {
            val urlList = ArrayList<String>()
            if (result is List<*>) {
                for (url in result as List<*>) {
                    val absoluteURL = NetworkUtils.getAbsoluteURL(redirectUrl, url.toString())
                    if (absoluteURL.isNotEmpty() && !urlList.contains(absoluteURL)) {
                        urlList.add(absoluteURL)
                    }
                }
            }
            return urlList
        }
        @Suppress("UNCHECKED_CAST")
        return result as? List<String>
    }

    /**
     * 获取文本
     */
    @JvmOverloads
    fun getString(ruleStr: String?, isUrl: Boolean = false, value: String? = null): String {
        if (TextUtils.isEmpty(ruleStr)) return ""
        val ruleList = splitSourceRule(ruleStr)
        return getString(ruleList, isUrl, value)
    }

    @JvmOverloads
    fun getString(
            ruleList: List<SourceRule>,
            isUrl: Boolean = false,
            value: String? = null
    ): String {
        var result: Any? = value
        val content = this.content
        if ((content != null || result != null) && ruleList.isNotEmpty()) {
            if (result == null) result = content
            if (result is NativeObject) {
                result = result[ruleList[0].rule]?.toString()
            } else {
                for (sourceRule in ruleList) {
                    putRule(sourceRule.putMap)
                    sourceRule.makeUpRule(result)
                    result?.let {
                        if (sourceRule.rule.isNotBlank() || sourceRule.replaceRegex.isEmpty()) {
                            result = when (sourceRule.mode) {
                                Mode.Js -> evalJS(sourceRule.rule, it)
                                Mode.Json -> getAnalyzeByJSonPath(it).getString(sourceRule.rule)
                                Mode.XPath -> getAnalyzeByXPath(it).getString(sourceRule.rule)
                                Mode.Default -> if (isUrl) {
                                    getAnalyzeByJSoup(it).getString0(sourceRule.rule)
                                } else {
                                    getAnalyzeByJSoup(it).getString(sourceRule.rule)
                                }
                                else -> sourceRule.rule
                            }
                        }
                        if ((result != null) && sourceRule.replaceRegex.isNotEmpty()) {
                            result = replaceRegex(result.toString(), sourceRule)
                        }
                    }
                }
            }
        }
        if (result == null) result = ""
        val str = kotlin.runCatching {
            Entities.unescape(result.toString())
        }.getOrElse {
            result.toString()
        }
        if (isUrl) {
            return if (str.isBlank()) {
                baseUrl ?: ""
            } else {
                NetworkUtils.getAbsoluteURL(redirectUrl, str)
            }
        }
        return str
    }

    /**
     * 获取Element
     */
    fun getElement(ruleStr: String): Any? {
        if (TextUtils.isEmpty(ruleStr)) return null
        var result: Any? = null
        val ruleList = splitSourceRule(ruleStr)
        content?.let { o ->
            if (ruleList.isNotEmpty()) result = o
            for (sourceRule in ruleList) {
                putRule(sourceRule.putMap)
                sourceRule.makeUpRule(result)
                result?.let {
                    result = when (sourceRule.mode) {
                        Mode.Regex -> AnalyzeByRegex.getElement(
                                result.toString(),
                                sourceRule.rule.splitNotBlank("&&")
                        )
                        Mode.Js -> evalJS(sourceRule.rule, it)
                        Mode.Json -> getAnalyzeByJSonPath(it).getObject(sourceRule.rule)
                        Mode.XPath -> getAnalyzeByXPath(it).getElements(sourceRule.rule)
                        else -> getAnalyzeByJSoup(it).getElements(sourceRule.rule)
                    }
                    if (sourceRule.replaceRegex.isNotEmpty()) {
                        result = replaceRegex(result.toString(), sourceRule)
                    }
                }
            }
        }
        return result
    }

    /**
     * 获取列表
     */
    @Suppress("UNCHECKED_CAST")
    fun getElements(ruleStr: String): List<Any> {
        var result: Any? = null
        val ruleList = splitSourceRule(ruleStr, true)
        content?.let { o ->
            if (ruleList.isNotEmpty()) result = o
            for (sourceRule in ruleList) {
                putRule(sourceRule.putMap)
                result?.let {
                    result = when (sourceRule.mode) {
                        Mode.Regex -> AnalyzeByRegex.getElements(
                                result.toString(),
                                sourceRule.rule.splitNotBlank("&&")
                        )
                        Mode.Js -> evalJS(sourceRule.rule, result)
                        Mode.Json -> getAnalyzeByJSonPath(it).getList(sourceRule.rule)
                        Mode.XPath -> getAnalyzeByXPath(it).getElements(sourceRule.rule)
                        else -> getAnalyzeByJSoup(it).getElements(sourceRule.rule)
                    }
                    if (sourceRule.replaceRegex.isNotEmpty()) {
                        result = replaceRegex(result.toString(), sourceRule)
                    }
                }
            }
        }
        result?.let {
            return it as List<Any>
        }
        return ArrayList()
    }

    /**
     * 保存变量
     */
    private fun putRule(map: Map<String, String>) {
        for ((key, value) in map) {
            put(key, getString(value))
        }
    }

    /**
     * 分离put规则
     */
    private fun splitPutRule(ruleStr: String, putMap: HashMap<String, String>): String {
        var vRuleStr = ruleStr
        val putMatcher = putPattern.matcher(vRuleStr)
        while (putMatcher.find()) {
            vRuleStr = vRuleStr.replace(putMatcher.group(), "")
            val map = GSON.fromJsonObject<Map<String, String>>(putMatcher.group(1))
            map?.let { putMap.putAll(map) }
        }
        return vRuleStr
    }

    /**
     * 正则替换
     */
    private fun replaceRegex(result: String, rule: SourceRule): String {
        var vResult = result
        if (rule.replaceRegex.isNotEmpty()) {
            vResult = if (rule.replaceFirst) {
                val pattern = Pattern.compile(rule.replaceRegex)
                val matcher = pattern.matcher(vResult)
                if (matcher.find()) {
                    matcher.group(0)!!.replaceFirst(rule.replaceRegex.toRegex(), rule.replacement)
                } else {
                    ""
                }
            } else {
                vResult.replace(rule.replaceRegex.toRegex(), rule.replacement)
            }
        }
        return vResult
    }

    /**
     * 分解规则生成规则列表
     */
    fun splitSourceRule(ruleStr: String?, isList: Boolean = false): List<SourceRule> {
        if (ruleStr.isNullOrEmpty()) return ArrayList<SourceRule>()
        val ruleList = ArrayList<SourceRule>()
        var mMode: Mode = Mode.Default
        var start = 0
        //仅首字符为:时为AllInOne，其实:与伪类选择器冲突，建议改成?更合理
        if (isList && ruleStr.startsWith(":")) {
            mMode = Mode.Regex
            isRegex = true
            start = 1
        } else if (isRegex) {
            mMode = Mode.Regex
        }
        var tmp: String
        val jsMatcher = JS_PATTERN.matcher(ruleStr)
        while (jsMatcher.find()) {
            if (jsMatcher.start() > start) {
                tmp = ruleStr.substring(start, jsMatcher.start()).trim { it <= ' ' }
                if (tmp.isNotEmpty()) {
                    ruleList.add(SourceRule(tmp, mMode))
                }
            }
            ruleList.add(SourceRule(jsMatcher.group(2) ?: jsMatcher.group(1), Mode.Js))
            start = jsMatcher.end()
        }

        if (ruleStr.length > start) {
            tmp = ruleStr.substring(start).trim { it <= ' ' }
            if (tmp.isNotEmpty()) {
                ruleList.add(SourceRule(tmp, mMode))
            }
        }

        return ruleList
    }

    /**
     * 规则类
     */

    inner class SourceRule internal constructor(
            ruleStr: String,
            internal var mode: Mode = Mode.Default
    ) {
        internal var rule: String
        internal var replaceRegex = ""
        internal var replacement = ""
        internal var replaceFirst = false
        internal val putMap = HashMap<String, String>()
        private val ruleParam = ArrayList<String>()
        private val ruleType = ArrayList<Int>()
        private val getRuleType = -2
        private val jsRuleType = -1
        private val defaultRuleType = 0

        init {
            rule = when {
                mode == Mode.Js || mode == Mode.Regex -> ruleStr
                ruleStr.startsWith("@XPath:", true) -> {
                    mode = Mode.XPath
                    ruleStr.substring(7)
                }
                ruleStr.startsWith("@Json:", true) -> {
                    mode = Mode.Json
                    ruleStr.substring(6)
                }
                ruleStr.startsWith("/") -> {//XPath特征很明显,无需配置单独的识别标头
                    mode = Mode.XPath
                    ruleStr
                }
                ruleStr.length > 1 && ruleStr[0] in ruleChar -> {
                    //isJSON为真时，<js></js>之后可能不是jsonPath,故提前判断
                    //XPath，Json已经提前判断了，故@开头即为默认规则，AnalyzeByJSoup会修剪开头的’@‘，故此处不必切片字符串
                    //#开头为根据id查元素,后续规则会切分##正则，故此处不必考虑##情况
                    //:开头为伪类，列表AllInOne已提前判断，此处是伪类无疑
                    mode = Mode.Default
                    ruleStr
                }
                isJSON || ruleStr.startsWith("$.") || ruleStr.startsWith("$[") -> {
                    mode = Mode.Json
                    ruleStr
                }
                else -> ruleStr
            }

            //分离put
            rule = splitPutRule(rule, putMap)
            //@get,{{ }}, 拆分
            var start = 0
            var tmp: String
            val evalMatcher = evalPattern.matcher(rule)

            if (evalMatcher.find()) {
                val mMode = mode == Mode.Js || mode == Mode.Regex
                if(!mMode) { //首次匹配，start为0
                    if(evalMatcher.start() == 0 //仅首次匹配时evalMatcher.start()可能为0
                            ||!rule.substring(0, evalMatcher.start()).contains("##") //此处start为0
                    )mode = Mode.Regex
                }
                do {
                    if (evalMatcher.start() > start) splitRegex( rule.substring(start, evalMatcher.start()) )
                    tmp = evalMatcher.group()
                    when {
                        tmp.startsWith("@get:", true) -> {
                            ruleType.add(getRuleType)
                            ruleParam.add(tmp.substring(6, tmp.lastIndex))
                        }
                        tmp.startsWith("{{") -> {
                            ruleType.add(jsRuleType)
                            ruleParam.add(tmp.substring(2, tmp.length - 2))
                        }
                        else -> {
                            splitRegex(tmp)
                        }
                    }
                    start = evalMatcher.end()
                } while (evalMatcher.find())
            }
            if (rule.length > start) {
                splitRegex( rule.substring(start) )
            }
        }

        /**
         * 拆分\$\d{1,2}
         */
        private fun splitRegex(ruleStr: String) {
            var start = 0
            var tmp: String
            val ruleStrArray = ruleStr.split("##")
            val regexMatcher = regexPattern.matcher(ruleStrArray[0])

            if (regexMatcher.find()) {
                if (mode != Mode.Js && mode != Mode.Regex) {
                    mode = Mode.Regex
                }
                do {
                    if (regexMatcher.start() > start) {
                        tmp = ruleStr.substring(start, regexMatcher.start())
                        ruleType.add(defaultRuleType)
                        ruleParam.add(tmp)
                    }
                    tmp = regexMatcher.group()
                    ruleType.add(tmp.substring(1).toInt())
                    ruleParam.add(tmp)
                    start = regexMatcher.end()
                } while (regexMatcher.find())
            }
            if (ruleStr.length > start) {
                tmp = ruleStr.substring(start)
                ruleType.add(defaultRuleType)
                ruleParam.add(tmp)
            }
        }

        /**
         * 替换@get,{{ }}
         */
        fun makeUpRule(result: Any?) {
            val infoVal = StringBuilder()
            if (ruleParam.isNotEmpty()) {
                var index = ruleParam.size
                while ( index-- > 0 ) {
                    val regType = ruleType[index]
                    when {
                        regType > defaultRuleType -> {
                            @Suppress("UNCHECKED_CAST")
                            (result as? List<String?>)?.run {
                                if (this.size > regType) {
                                    this[regType]?.let {
                                        infoVal.insert(0, it)
                                    }
                                }
                            }?:infoVal.insert(0, ruleParam[index])
                        }
                        regType == jsRuleType -> {
                            if (isRule(ruleParam[index])) {
                                infoVal.insert(0, getString(arrayListOf(SourceRule(ruleParam[index]))))
                            } else {
                                evalJS(ruleParam[index], result)?.run{
                                    infoVal.insert(0, when(this){
                                        is String -> this
                                        this is Double && this % 1.0 == 0.0 -> String.format("%.0f", this)
                                        else ->  toString()
                                    })
                                }
                            }
                        }
                        regType == getRuleType -> {
                            infoVal.insert(0, get(ruleParam[index]))
                        }
                        else -> infoVal.insert(0, ruleParam[index])
                    }
                }
                rule = infoVal.toString()
            }
            //分离正则表达式
            val ruleStrS = rule.split("##")
            rule = ruleStrS[0].trim()
            if (ruleStrS.size > 1) {
                replaceRegex = ruleStrS[1]
            }
            if (ruleStrS.size > 2) {
                replacement = ruleStrS[2]
            }
            if (ruleStrS.size > 3) {
                replaceFirst = true
            }
        }

        private fun isRule(ruleStr: String): Boolean {
            return ruleStr.startsWith("//")
                    || ruleStr.startsWith("$.")
                    || ruleStr.startsWith("$[")
                    || ruleStr.length > 1 && ruleStr[0] in innerRuleChar
        }
    }

    enum class Mode {
        XPath, Json, Default, Js, Regex
    }

    fun put(key: String, value: String): String {
        chapter?.putVariable(key, value)
                ?: book?.putVariable(key, value)
                ?: ruleData.putVariable(key, value)
        return value
    }

    fun get(key: String): String {
        when (key) {
            "bookName" -> book?.let {
                return it.name
            }
            "title" -> chapter?.let {
                return it.title
            }
        }
        return chapter?.variableMap?.get(key)
                ?: book?.variableMap?.get(key)
                ?: ruleData.variableMap[key]
                ?: ""
    }

    /**
     * 执行JS
     */
    fun evalJS(jsStr: String, result: Any?): Any? {
        return SCRIPT_ENGINE.eval(jsStr, SimpleBindings().also{
            it["java"] = this
            it["cookie"] = CookieStore
            it["cache"] = CacheManager
            it["book"] = book
            it["result"] = result
            it["baseUrl"] = baseUrl
            it["chapter"] = chapter
            it["title"] = chapter?.title
            it["src"] = content
            it["nextChapterUrl"] = nextChapterUrl
        })
    }

    /**
     * js实现跨域访问,不能删
     */
    override fun ajax(urlStr: String): String? {
        return runBlocking {
            kotlin.runCatching {
                AnalyzeUrl(urlStr, book = book).getStrResponse(urlStr).body
            }.onFailure {
                it.printStackTrace()
            }.getOrElse {
                it.msg
            }
        }
    }

    /**
     * 章节数转数字
     */
    fun toNumChapter(s: String?): String? {
        s ?: return null
        val matcher = titleNumPattern.matcher(s)
        if (matcher.find()) {
            return "第${StringUtils.stringToInt(matcher.group(1))}${matcher.group(2)}"
        }
        return s
    }

    companion object {
        private val ruleChar = listOf('@','.',':','>','#','[')
        private val innerRuleChar = listOf('@','.',':','>','#','*')
        private val putPattern = Pattern.compile("@put:(\\{[^}]+?\\})", Pattern.CASE_INSENSITIVE)
        private val evalPattern =
                Pattern.compile("@get:\\{[^}]+?\\}|\\{\\{[\\w\\W]*?\\}\\}", Pattern.CASE_INSENSITIVE)
        private val regexPattern = Pattern.compile("\\$\\d{1,2}")
        private val titleNumPattern = Pattern.compile("第(.+?)([章节回话篇])")
    }

}
