package io.legado.app.model.analyzeRule

import android.text.TextUtils
import androidx.annotation.Keep
import com.script.CompiledScript
import com.script.buildScriptBindings
import com.script.rhino.RhinoScriptEngine
import io.legado.app.constant.AppPattern.JS_PATTERN
import io.legado.app.data.entities.BaseBook
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.RssArticle
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.CacheManager
import io.legado.app.help.JsExtensions
import io.legado.app.help.http.CookieStore
import io.legado.app.help.source.getShareScope
import io.legado.app.model.Debug
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.GSON
import io.legado.app.utils.GSONStrict
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.getOrPutLimit
import io.legado.app.utils.isDataUrl
import io.legado.app.utils.isJson
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.splitNotBlank
import io.legado.app.utils.stackTraceStr
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.nodes.Node
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.Scriptable
import java.lang.ref.WeakReference
import java.net.URL
import java.util.Locale
import java.util.regex.Pattern
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 解析规则获取结果
 */
@Keep
@Suppress("unused", "RegExpRedundantEscape", "MemberVisibilityCanBePrivate")
class AnalyzeRule(
    private var ruleData: RuleDataInterface? = null,
    private val source: BaseSource? = null,
    private val preUpdateJs: Boolean = false
) : JsExtensions {

    private val book get() = ruleData as? BaseBook
    private val rssArticle get() = ruleData as? RssArticle

    private var chapter: BookChapter? = null
    private var nextChapterUrl: String? = null
    private var content: Any? = null
    var baseUrl: String? = null
    var redirectUrl: URL? = null
    private var isJSON: Boolean = false
    private var isRegex: Boolean = false

    private var analyzeByXPath: AnalyzeByXPath? = null
    private var analyzeByJSoup: AnalyzeByJSoup? = null
    private var analyzeByJSonPath: AnalyzeByJSonPath? = null

    private val stringRuleCache = hashMapOf<String, List<SourceRule>>()
    private val regexCache = hashMapOf<String, Regex?>()
    private val scriptCache = hashMapOf<String, CompiledScript>()
    private var topScopeRef: WeakReference<Scriptable>? = null
    private var evalJSCallCount = 0

    private var coroutineContext: CoroutineContext = EmptyCoroutineContext

    private var loggedNonStandardJSON = false

    @JvmOverloads
    fun setContent(content: Any?, baseUrl: String? = null): AnalyzeRule {
        if (content == null) throw AssertionError("内容不可空（Content cannot be null）")
        this.content = content
        isJSON = when (content) {
            is Node -> false
            else -> content.toString().isJson()
        }
        setBaseUrl(baseUrl)
        analyzeByXPath = null
        analyzeByJSoup = null
        analyzeByJSonPath = null
        return this
    }

    fun setBaseUrl(baseUrl: String?): AnalyzeRule {
        baseUrl?.let {
            this.baseUrl = baseUrl
        }
        return this
    }

    fun setRedirectUrl(url: String): URL? {
        if (url.isDataUrl()) {
            return redirectUrl
        }
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
            if (analyzeByXPath == null) {
                analyzeByXPath = AnalyzeByXPath(content!!)
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
            if (analyzeByJSoup == null) {
                analyzeByJSoup = AnalyzeByJSoup(content!!)
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
            if (analyzeByJSonPath == null) {
                analyzeByJSonPath = AnalyzeByJSonPath(content!!)
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
        val ruleList = splitSourceRuleCacheString(rule)
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
            if (result is NativeObject) {
                val sourceRule = ruleList.first()
                putRule(sourceRule.putMap)
                sourceRule.makeUpRule(result)
                result = if (sourceRule.getParamSize() > 1) {
                    // get {{}}
                    sourceRule.rule
                } else {
                    // 键值直接访问
                    result[sourceRule.rule]
                }
                result?.let {
                    if (sourceRule.replaceRegex.isNotEmpty() && it is List<*>) {
                        result = it.map { o ->
                            replaceRegex(o.toString(), sourceRule)
                        }
                    } else if (sourceRule.replaceRegex.isNotEmpty()) {
                        result = replaceRegex(result.toString(), sourceRule)
                    }
                }
            } else {
                for (sourceRule in ruleList) {
                    putRule(sourceRule.putMap)
                    sourceRule.makeUpRule(result)
                    result ?: continue
                    val rule = sourceRule.rule
                    if (rule.isNotEmpty()) {
                        result = when (sourceRule.mode) {
                            Mode.Js -> evalJS(rule, result)
                            Mode.Json -> getAnalyzeByJSonPath(result).getStringList(rule)
                            Mode.XPath -> getAnalyzeByXPath(result).getStringList(rule)
                            Mode.Default -> getAnalyzeByJSoup(result).getStringList(rule)
                            else -> rule
                        }
                    }
                    if (sourceRule.replaceRegex.isNotEmpty() && result is List<*>) {
                        val newList = ArrayList<String>()
                        for (item in result) {
                            newList.add(replaceRegex(item.toString(), sourceRule))
                        }
                        result = newList
                    } else if (sourceRule.replaceRegex.isNotEmpty()) {
                        result = replaceRegex(result.toString(), sourceRule)
                    }
                }
            }
        }
        if (result == null) return null
        if (result is String) {
            result = result.split("\n")
        }
        if (isUrl) {
            val urlList = ArrayList<String>()
            if (result is List<*>) {
                for (url in result) {
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
        val ruleList = splitSourceRuleCacheString(ruleStr)
        return getString(ruleList, mContent, isUrl)
    }

    fun getString(ruleStr: String?, unescape: Boolean): String {
        if (TextUtils.isEmpty(ruleStr)) return ""
        val ruleList = splitSourceRuleCacheString(ruleStr)
        return getString(ruleList, unescape = unescape)
    }

    @JvmOverloads
    fun getString(
        ruleList: List<SourceRule>,
        mContent: Any? = null,
        isUrl: Boolean = false,
        unescape: Boolean = true
    ): String {
        var result: Any? = null
        val content = mContent ?: this.content
        if (content != null && ruleList.isNotEmpty()) {
            result = content
            if (result is NativeObject) {
                val sourceRule = ruleList.first()
                putRule(sourceRule.putMap)
                sourceRule.makeUpRule(result)
                result = if (sourceRule.getParamSize() > 1) {
                    // get {{}}
                    sourceRule.rule
                } else {
                    // 键值直接访问
                    result[sourceRule.rule]?.toString()
                }?.let {
                    replaceRegex(it, sourceRule)
                }
            } else {
                for (sourceRule in ruleList) {
                    putRule(sourceRule.putMap)
                    sourceRule.makeUpRule(result)
                    result ?: continue
                    val rule = sourceRule.rule
                    if (rule.isNotBlank() || sourceRule.replaceRegex.isEmpty()) {
                        result = when (sourceRule.mode) {
                            Mode.Js -> evalJS(rule, result)
                            Mode.Json -> getAnalyzeByJSonPath(result).getString(rule)
                            Mode.XPath -> getAnalyzeByXPath(result).getString(rule)
                            Mode.Default -> if (isUrl) {
                                getAnalyzeByJSoup(result).getString0(rule)
                            } else {
                                getAnalyzeByJSoup(result).getString(rule)
                            }

                            else -> rule
                        }
                    }
                    if (result != null && sourceRule.replaceRegex.isNotEmpty()) {
                        result = replaceRegex(result.toString(), sourceRule)
                    }
                }
            }
        }
        if (result == null) result = ""
        val resultStr = result.toString()
        val str = if (unescape && resultStr.indexOf('&') > -1) {
            StringEscapeUtils.unescapeHtml4(resultStr)
        } else {
            resultStr
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
                result ?: continue
                val rule = sourceRule.rule
                result = when (sourceRule.mode) {
                    Mode.Regex -> AnalyzeByRegex.getElement(
                        result.toString(),
                        rule.splitNotBlank("&&")
                    )

                    Mode.Js -> evalJS(rule, result)
                    Mode.Json -> getAnalyzeByJSonPath(result).getObject(rule)
                    Mode.XPath -> getAnalyzeByXPath(result).getElements(rule)
                    else -> getAnalyzeByJSoup(result).getElements(rule)
                }
                if (sourceRule.replaceRegex.isNotEmpty()) {
                    result = replaceRegex(result.toString(), sourceRule)
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
                result ?: continue
                val rule = sourceRule.rule
                result = when (sourceRule.mode) {
                    Mode.Regex -> AnalyzeByRegex.getElements(
                        result.toString(),
                        rule.splitNotBlank("&&")
                    )

                    Mode.Js -> evalJS(rule, result)
                    Mode.Json -> getAnalyzeByJSonPath(result).getList(rule)
                    Mode.XPath -> getAnalyzeByXPath(result).getElements(rule)
                    else -> getAnalyzeByJSoup(result).getElements(rule)
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
            val putJsonStr = putMatcher.group(1)
            val putJson = GSONStrict.fromJsonObject<Map<String, String>>(putJsonStr)
                .getOrNull()
            if (putJson != null) {
                putMap.putAll(putJson)
                continue
            }
            GSON.fromJsonObject<Map<String, String>>(putJsonStr)
                .getOrNull()
                ?.let {
                    if (!loggedNonStandardJSON) {
                        Debug.log("≡@put 规则 JSON 格式不规范，请改为规范格式")
                        loggedNonStandardJSON = true
                    }
                    putMap.putAll(it)
                }
        }
        return vRuleStr
    }

    /**
     * 正则替换
     */
    private fun replaceRegex(result: String, rule: SourceRule): String {
        if (rule.replaceRegex.isEmpty()) return result
        val replaceRegex = rule.replaceRegex
        val replacement = rule.replacement
        val regex = compileRegexCache(replaceRegex)
        if (rule.replaceFirst) {
            /* ##match##replace### 获取第一个匹配到的结果并进行替换 */
            if (regex != null) kotlin.runCatching {
                val pattern = regex.toPattern()
                val matcher = pattern.matcher(result)
                return if (matcher.find()) {
                    matcher.group(0)!!.replaceFirst(regex, replacement)
                } else {
                    ""
                }
            }
            return replacement
        } else {
            /* ##match##replace 替换*/
            if (regex != null) kotlin.runCatching {
                return result.replace(regex, replacement)
            }
            return result.replace(replaceRegex, replacement)
        }
    }

    private fun compileRegexCache(regex: String): Regex? {
        return regexCache.getOrPutLimit(regex, 16) {
            try {
                regex.toRegex()
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * getString 类规则缓存
     */
    private fun splitSourceRuleCacheString(ruleStr: String?): List<SourceRule> {
        if (ruleStr.isNullOrEmpty()) return emptyList()
        return stringRuleCache.getOrPut(ruleStr) {
            splitSourceRule(ruleStr)
        }
    }

    /**
     * 分解规则生成规则列表
     */
    fun splitSourceRule(ruleStr: String?, allInOne: Boolean = false): List<SourceRule> {
        if (ruleStr.isNullOrEmpty()) return emptyList()
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

    private fun getOrCreateSingleSourceRule(rule: String): List<SourceRule> {
        return stringRuleCache.getOrPutLimit(rule, 16) {
            listOf(SourceRule(rule))
        }
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
                                val ruleList = getOrCreateSingleSourceRule(ruleParam[index])
                                getString(ruleList).let {
                                    infoVal.insert(0, it)
                                }
                            } else {
                                val jsEval: Any? = evalJS(ruleParam[index], result)
                                when {
                                    jsEval == null -> Unit
                                    jsEval is String -> infoVal.insert(0, jsEval)
                                    jsEval is Double && jsEval % 1.0 == 0.0 -> infoVal.insert(
                                        0,
                                        String.format(Locale.ROOT, "%.0f", jsEval)
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

        fun getParamSize(): Int {
            return ruleParam.size
        }
    }

    enum class Mode {
        XPath, Json, Default, Js, Regex
    }

    /**
     * 保存数据
     */
    fun put(key: String, value: String): String {
        chapter?.putVariable(key, value)
            ?: book?.putVariable(key, value)
            ?: ruleData?.putVariable(key, value)
            ?: source?.put(key, value)
        return value
    }

    /**
     * 获取保存的数据
     */
    fun get(key: String): String {
        when (key) {
            "bookName" -> book?.let {
                return it.name
            }

            "title" -> chapter?.let {
                return it.title
            }
        }
        return chapter?.getVariable(key)?.takeIf { it.isNotEmpty() }
            ?: book?.getVariable(key)?.takeIf { it.isNotEmpty() }
            ?: ruleData?.getVariable(key)?.takeIf { it.isNotEmpty() }
            ?: source?.get(key)?.takeIf { it.isNotEmpty() }
            ?: ""
    }

    /**
     * 执行JS
     */
    fun evalJS(jsStr: String, result: Any? = null): Any? {
        val bindings = buildScriptBindings { bindings ->
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
            bindings["rssArticle"] = rssArticle
        }
        val topScope = source?.getShareScope(coroutineContext) ?: topScopeRef?.get()
        val scope = if (topScope == null) {
            RhinoScriptEngine.getRuntimeScope(bindings).apply {
                if (evalJSCallCount++ > 16) {
                    topScopeRef = WeakReference(prototype)
                }
            }
        } else {
            bindings.apply {
                prototype = topScope
            }
        }
        val script = compileScriptCache(jsStr)
        val result = script.eval(scope, coroutineContext)
        return result
    }

    private fun compileScriptCache(jsStr: String): CompiledScript {
        return scriptCache.getOrPutLimit(jsStr, 16) {
            RhinoScriptEngine.compile(jsStr)
        }
    }

    override fun getSource(): BaseSource? {
        return source
    }

    /**
     * js实现跨域访问,不能删
     */
    override fun ajax(url: Any): String? {
        val urlStr = if (url is List<*>) {
            url.firstOrNull().toString()
        } else {
            url.toString()
        }
        val analyzeUrl = AnalyzeUrl(
            urlStr,
            source = source,
            ruleData = book,
            coroutineContext = coroutineContext
        )
        return kotlin.runCatching {
            analyzeUrl.getStrResponse().body
        }.onFailure {
            coroutineContext.ensureActive()
            log("ajax(${urlStr}) error\n${it.stackTraceToString()}")
            it.printOnDebug()
        }.getOrElse {
            it.stackTraceStr
        }
    }

    /**
     * 重新获取book
     */
    fun reGetBook() {
        if (!preUpdateJs) throw NoStackTraceException("只能在 preUpdateJs 中调用")
        val bookSource = source as? BookSource
        val book = book as? Book
        if (bookSource == null || book == null) return
        runBlocking(coroutineContext) {
            withTimeout(1800000) {
                WebBook.preciseSearchAwait(bookSource, book.name, book.author)
                    .getOrThrow().let {
                        book.bookUrl = it.bookUrl
                        it.variableMap.forEach { entry ->
                            book.putVariable(entry.key, entry.value)
                        }
                    }
                WebBook.getBookInfoAwait(bookSource, book, false)
            }
        }
    }

    /**
     * 更新tocUrl,有些书源目录url定期更新,可以在js调用更新
     */
    fun refreshTocUrl() {
        if (!preUpdateJs) throw NoStackTraceException("只能在 preUpdateJs 中调用")
        val bookSource = source as? BookSource
        val book = book as? Book
        if (bookSource == null || book == null) return
        runBlocking(coroutineContext) {
            withTimeout(1800000) {
                WebBook.getBookInfoAwait(bookSource, book, false)
            }
        }
    }

    companion object {
        private val putPattern = Pattern.compile("@put:(\\{[^}]+?\\})", Pattern.CASE_INSENSITIVE)
        private val evalPattern =
            Pattern.compile("@get:\\{[^}]+?\\}|\\{\\{[\\w\\W]*?\\}\\}", Pattern.CASE_INSENSITIVE)
        private val regexPattern = Pattern.compile("\\$\\d{1,2}")

        fun AnalyzeRule.setCoroutineContext(context: CoroutineContext): AnalyzeRule {
            coroutineContext = context.minusKey(ContinuationInterceptor)
            return this
        }

        fun AnalyzeRule.setRuleData(ruleData: RuleDataInterface?): AnalyzeRule {
            this.ruleData = ruleData
            return this
        }

        fun AnalyzeRule.setNextChapterUrl(nextChapterUrl: String?): AnalyzeRule {
            this.nextChapterUrl = nextChapterUrl
            return this
        }

        fun AnalyzeRule.setChapter(chapter: BookChapter?): AnalyzeRule {
            this.chapter = chapter
            return this
        }

    }

}
