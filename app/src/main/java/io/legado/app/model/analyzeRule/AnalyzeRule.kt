package io.legado.app.model.analyzeRule

import android.text.TextUtils
import androidx.annotation.Keep
import io.legado.app.constant.AppConst.SCRIPT_ENGINE
import io.legado.app.constant.AppPattern.JS_PATTERN
import io.legado.app.data.entities.BaseBook
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.CacheManager
import io.legado.app.help.JsExtensions
import io.legado.app.help.http.CookieStore
import io.legado.app.utils.*
import kotlinx.coroutines.runBlocking
import org.jsoup.nodes.Entities
import org.mozilla.javascript.NativeObject
import timber.log.Timber
import java.net.URL
import java.util.*
import java.util.regex.Pattern
import javax.script.SimpleBindings
import kotlin.collections.HashMap

/**
 * 解析规则获取结果
 */
@Keep
@Suppress("unused", "RegExpRedundantEscape", "MemberVisibilityCanBePrivate")
class AnalyzeRule(
    val ruleData: RuleDataInterface,
    private val source: BaseSource? = null
) : JsExtensions {

    var book = if (ruleData is BaseBook) ruleData else null

    var chapter: BookChapter? = null
    var nextChapterUrl: String? = null
    var content: Any? = null
        private set
    var baseUrl: String? = null
        private set
    var redirectUrl: URL? = null
        private set
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
        try {
            redirectUrl = URL(url)
        } catch (e: Exception) {
            log("URL($url) error\n${e.localizedMessage}")
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
    fun getStringList(rule: String?, mContent: Any? = null, isUrl: Boolean = false): List<String>? {
        if (rule.isNullOrEmpty()) return null
        val ruleList = splitSourceRule(rule, false)
        return getStringList(ruleList, mContent, isUrl)
    }

    @JvmOverloads
    fun getStringList(
        ruleList: List<SourceRule>,
        mContent: Any? = null,
        isUrl: Boolean = false
    ): List<String>? {
        var result: Any? = null
        val content = mContent ?: this.content
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
    fun getString(ruleStr: String?, mContent: Any? = null, isUrl: Boolean = false): String {
        if (TextUtils.isEmpty(ruleStr)) return ""
        val ruleList = splitSourceRule(ruleStr)
        return getString(ruleList, mContent, isUrl)
    }

    @JvmOverloads
    fun getString(
        ruleList: List<SourceRule>,
        mContent: Any? = null,
        isUrl: Boolean = false
    ): String {
        var result: Any? = null
        val content = mContent ?: this.content
        if (content != null && ruleList.isNotEmpty()) {
            result = content
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
        }.onFailure {
            log("Entities.unescape() error\n${it.localizedMessage}")
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
        val content = this.content
        val ruleList = splitSourceRule(ruleStr, true)
        if (content != null && ruleList.isNotEmpty()) {
            result = content
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
        val content = this.content
        val ruleList = splitSourceRule(ruleStr, true)
        if (content != null && ruleList.isNotEmpty()) {
            result = content
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
        if (rule.replaceRegex.isEmpty()) return result
        var vResult = result
        vResult = if (rule.replaceFirst) {
            kotlin.runCatching {
                val pattern = Pattern.compile(rule.replaceRegex)
                val matcher = pattern.matcher(vResult)
                if (matcher.find()) {
                    matcher.group(0)!!.replaceFirst(rule.replaceRegex.toRegex(), rule.replacement)
                } else {
                    ""
                }
            }.getOrElse {
                vResult.replaceFirst(rule.replaceRegex, rule.replacement)
            }
        } else {
            kotlin.runCatching {
                vResult.replace(rule.replaceRegex.toRegex(), rule.replacement)
            }.getOrElse {
                vResult.replace(rule.replaceRegex, rule.replacement)
            }
        }
        return vResult
    }

    /**
     * 分解规则生成规则列表
     */
    fun splitSourceRule(ruleStr: String?, allInOne: Boolean = false): List<SourceRule> {
        if (ruleStr.isNullOrEmpty()) return ArrayList<SourceRule>()
        val ruleList = ArrayList<SourceRule>()
        var mMode: Mode = Mode.Default
        var start = 0
        //仅首字符为:时为AllInOne，其实:与伪类选择器冲突，建议改成?更合理
        if (allInOne && ruleStr.startsWith(":")) {
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
                ruleStr.startsWith("@CSS:", true) -> {
                    mode = Mode.Default
                    ruleStr
                }
                ruleStr.startsWith("@@") -> {
                    mode = Mode.Default
                    ruleStr.substring(2)
                }
                ruleStr.startsWith("@XPath:", true) -> {
                    mode = Mode.XPath
                    ruleStr.substring(7)
                }
                ruleStr.startsWith("@Json:", true) -> {
                    mode = Mode.Json
                    ruleStr.substring(6)
                }
                isJSON || ruleStr.startsWith("$.") || ruleStr.startsWith("$[") -> {
                    mode = Mode.Json
                    ruleStr
                }
                ruleStr.startsWith("/") -> {//XPath特征很明显,无需配置单独的识别标头
                    mode = Mode.XPath
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
                tmp = rule.substring(start, evalMatcher.start())
                if (mode != Mode.Js && mode != Mode.Regex &&
                    (evalMatcher.start() == 0 || !tmp.contains("##"))
                ) {
                    mode = Mode.Regex
                }
                do {
                    if (evalMatcher.start() > start) {
                        tmp = rule.substring(start, evalMatcher.start())
                        splitRegex(tmp)
                    }
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
                tmp = rule.substring(start)
                splitRegex(tmp)
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
                while (index-- > 0) {
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
                            } ?: infoVal.insert(0, ruleParam[index])
                        }
                        regType == jsRuleType -> {
                            if (isRule(ruleParam[index])) {
                                getString(arrayListOf(SourceRule(ruleParam[index]))).let {
                                    infoVal.insert(0, it)
                                }
                            } else {
                                val jsEval: Any? = evalJS(ruleParam[index], result)
                                when {
                                    jsEval == null -> Unit
                                    jsEval is String -> infoVal.insert(0, jsEval)
                                    jsEval is Double && jsEval % 1.0 == 0.0 -> infoVal.insert(
                                        0,
                                        String.format("%.0f", jsEval)
                                    )
                                    else -> infoVal.insert(0, jsEval.toString())
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
            return ruleStr.startsWith('@') //js首个字符不可能是@，除非是装饰器，所以@开头规定为规则
                    || ruleStr.startsWith("$.")
                    || ruleStr.startsWith("$[")
                    || ruleStr.startsWith("//")
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
        val bindings = SimpleBindings()
        bindings["java"] = this
        bindings["cookie"] = CookieStore
        bindings["cache"] = CacheManager
        bindings["source"] = source
        bindings["book"] = book
        bindings["result"] = result
        bindings["baseUrl"] = baseUrl
        bindings["chapter"] = chapter
        bindings["title"] = chapter?.title
        bindings["src"] = content
        bindings["nextChapterUrl"] = nextChapterUrl
        return SCRIPT_ENGINE.eval(jsStr, bindings)
    }

    override fun getSource(): BaseSource? {
        return source
    }

    /**
     * js实现跨域访问,不能删
     */
    override fun ajax(urlStr: String): String? {
        return runBlocking {
            kotlin.runCatching {
                val analyzeUrl = AnalyzeUrl(urlStr, source = source, ruleData = book)
                analyzeUrl.getStrResponseAwait().body
            }.onFailure {
                log("ajax(${urlStr}) error\n${it.stackTraceToString()}")
                Timber.e(it)
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
            return "${matcher.group(1)}${StringUtils.stringToInt(matcher.group(2))}${matcher.group(3)}"
        }
        return s
    }

    companion object {
        private val putPattern = Pattern.compile("@put:(\\{[^}]+?\\})", Pattern.CASE_INSENSITIVE)
        private val evalPattern =
            Pattern.compile("@get:\\{[^}]+?\\}|\\{\\{[\\w\\W]*?\\}\\}", Pattern.CASE_INSENSITIVE)
        private val regexPattern = Pattern.compile("\\$\\d{1,2}")
        private val titleNumPattern = Pattern.compile("(第)(.+?)(章)")
    }

}
