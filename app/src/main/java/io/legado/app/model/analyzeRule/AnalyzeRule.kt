package io.legado.app.model.analyzeRule

import android.text.TextUtils
import androidx.annotation.Keep
import io.legado.app.constant.AppConst.SCRIPT_ENGINE
import io.legado.app.constant.Pattern.JS_PATTERN
import io.legado.app.data.entities.BaseBook
import io.legado.app.utils.*
import org.mozilla.javascript.NativeObject
import java.util.*
import java.util.regex.Pattern
import javax.script.SimpleBindings
import kotlin.collections.HashMap


/**
 * Created by REFGD.
 * 统一解析接口
 */
@Keep
@Suppress("unused")
class AnalyzeRule(private var book: BaseBook? = null) {
    private var content: Any? = null
    private var baseUrl: String? = null
    private var isJSON: Boolean = false
    private var isRegex: Boolean = false

    private var analyzeByXPath: AnalyzeByXPath? = null
    private var analyzeByJSoup: AnalyzeByJSoup? = null
    private var analyzeByJSonPath: AnalyzeByJSonPath? = null

    private var objectChangedXP = false
    private var objectChangedJS = false
    private var objectChangedJP = false

    fun setBook(book: BaseBook) {
        this.book = book
    }

    @Throws(Exception::class)
    @JvmOverloads
    fun setContent(content: Any?, baseUrl: String? = this.baseUrl): AnalyzeRule {
        if (content == null) throw AssertionError("Content cannot be null")
        isJSON = content.toString().isJson()
        this.content = content
        this.baseUrl = baseUrl
        objectChangedXP = true
        objectChangedJS = true
        objectChangedJP = true
        return this
    }

    /**
     * 获取XPath解析类
     */
    private fun getAnalyzeByXPath(o: Any): AnalyzeByXPath {
        return if (o != content) {
            AnalyzeByXPath().parse(o)
        } else getAnalyzeByXPath()
    }

    private fun getAnalyzeByXPath(): AnalyzeByXPath {
        if (analyzeByXPath == null || objectChangedXP) {
            analyzeByXPath = AnalyzeByXPath()
            analyzeByXPath?.parse(content!!)
            objectChangedXP = false
        }
        return analyzeByXPath as AnalyzeByXPath
    }

    /**
     * 获取JSOUP解析类
     */
    private fun getAnalyzeByJSoup(o: Any): AnalyzeByJSoup {
        return if (o != content) {
            AnalyzeByJSoup().parse(o)
        } else getAnalyzeByJSoup()
    }

    private fun getAnalyzeByJSoup(): AnalyzeByJSoup {
        if (analyzeByJSoup == null || objectChangedJS) {
            analyzeByJSoup = AnalyzeByJSoup()
            analyzeByJSoup?.parse(content!!)
            objectChangedJS = false
        }
        return analyzeByJSoup as AnalyzeByJSoup
    }

    /**
     * 获取JSON解析类
     */
    private fun getAnalyzeByJSonPath(o: Any): AnalyzeByJSonPath {
        return if (o != content) {
            AnalyzeByJSonPath().parse(o)
        } else getAnalyzeByJSonPath()
    }

    private fun getAnalyzeByJSonPath(): AnalyzeByJSonPath {
        if (analyzeByJSonPath == null || objectChangedJP) {
            analyzeByJSonPath = AnalyzeByJSonPath()
            analyzeByJSonPath?.parse(content!!)
            objectChangedJP = false
        }
        return analyzeByJSonPath as AnalyzeByJSonPath
    }

    /**
     * 获取文本列表
     */
    @Throws(Exception::class)
    @JvmOverloads
    fun getStringList(rule: String, isUrl: Boolean = false): List<String>? {
        if (TextUtils.isEmpty(rule)) return null
        val ruleList = splitSourceRule(rule)
        return getStringList(ruleList, isUrl)
    }

    @Throws(Exception::class)
    fun getStringList(ruleList: List<SourceRule>, isUrl: Boolean): List<String>? {
        var result: Any? = null
        content?.let { o ->
            if (ruleList.isNotEmpty()) {
                if (ruleList.isNotEmpty()) result = o
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
        if (result == null) return ArrayList()
        if (result is String) {
            result = listOf((result as String).htmlFormat().split("\n"))
        }
        if (isUrl) {
            val urlList = ArrayList<String>()
            if (result is List<*>) {
                for (url in result as List<*>) {
                    val absoluteURL = NetworkUtils.getAbsoluteURL(baseUrl, url.toString())
                    if (!absoluteURL.isNullOrEmpty() && !urlList.contains(absoluteURL)) {
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
    @Throws(Exception::class)
    fun getString(rule: String): String? {
        return getString(rule, false)
    }

    @Throws(Exception::class)
    fun getString(ruleStr: String, isUrl: Boolean): String? {
        if (TextUtils.isEmpty(ruleStr)) return null
        val ruleList = splitSourceRule(ruleStr)
        return getString(ruleList, isUrl)
    }

    @Throws(Exception::class)
    @JvmOverloads
    fun getString(ruleList: List<SourceRule>, isUrl: Boolean = false): String {
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
                        if (sourceRule.rule.isNotBlank()) {
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
                        if (sourceRule.replaceRegex.isNotEmpty()) {
                            result = replaceRegex(result.toString(), sourceRule)
                        }
                    }
                }
            }
        }
        if (result == null) result = ""
        if (isUrl) {
            return NetworkUtils.getAbsoluteURL(baseUrl, result.toString()) ?: ""
        }
        return result.toString()
    }

    /**
     * 获取Element
     */
    @Throws(Exception::class)
    fun getElement(ruleStr: String): Any? {
        if (TextUtils.isEmpty(ruleStr)) return null
        var result: Any? = null
        val ruleList = splitSourceRule(ruleStr)
        content?.let { o ->
            if (ruleList.isNotEmpty()) result = o
            for (sourceRule in ruleList) {
                putRule(sourceRule.putMap)
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
    @Throws(Exception::class)
    fun getElements(ruleStr: String): List<Any> {
        var result: Any? = null
        val ruleList = splitSourceRule(ruleStr)
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
    @Throws(Exception::class)
    private fun putRule(map: Map<String, String>) {
        for ((key, value) in map) {
            getString(value)?.let {
                book?.putVariable(key, it)
            }
        }
    }

    /**
     * 分离put规则
     */
    @Throws(Exception::class)
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
                    matcher.group(0).replaceFirst(rule.replaceRegex.toRegex(), rule.replacement)
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
    @Throws(Exception::class)
    fun splitSourceRule(ruleStr: String, mode: Mode = Mode.Default): List<SourceRule> {
        var vRuleStr = ruleStr
        val ruleList = ArrayList<SourceRule>()
        if (TextUtils.isEmpty(vRuleStr)) return ruleList
        //检测Mode
        var mMode: Mode = mode
        when {
            vRuleStr.startsWith("@XPath:", true) -> {
                mMode = Mode.XPath
                vRuleStr = vRuleStr.substring(7)
            }
            vRuleStr.startsWith("@Json:", true) -> {
                mMode = Mode.Json
                vRuleStr = vRuleStr.substring(6)
            }
            vRuleStr.startsWith(":") -> {
                mMode = Mode.Regex
                isRegex = true
                vRuleStr = vRuleStr.substring(1)
            }
            isRegex -> mMode = Mode.Regex
            isJSON -> mMode = Mode.Json
        }
        //拆分为规则列表
        var start = 0
        var tmp: String
        val jsMatcher = JS_PATTERN.matcher(vRuleStr)
        while (jsMatcher.find()) {
            if (jsMatcher.start() > start) {
                tmp = vRuleStr.substring(start, jsMatcher.start()).replace("\n", "")
                    .trim { it <= ' ' }
                if (!TextUtils.isEmpty(tmp)) {
                    ruleList.add(SourceRule(tmp, mMode))
                }
            }
            ruleList.add(SourceRule(jsMatcher.group(), Mode.Js))
            start = jsMatcher.end()
        }
        if (vRuleStr.length > start) {
            tmp = vRuleStr.substring(start).replace("\n", "").trim { it <= ' ' }
            if (!TextUtils.isEmpty(tmp)) {
                ruleList.add(SourceRule(tmp, mMode))
            }
        }
        return ruleList
    }

    /**
     * 规则类
     */
    inner class SourceRule internal constructor(ruleStr: String, mainMode: Mode) {
        internal var mode: Mode
        internal var rule: String
        internal var replaceRegex = ""
        internal var replacement = ""
        internal var replaceFirst = false
        internal val putMap = HashMap<String, String>()
        private val ruleParam = ArrayList<String>()
        private val ruleType = ArrayList<Int>()

        init {
            this.mode = mainMode
            if (mode == Mode.Js) {
                rule = if (ruleStr.startsWith("<js>")) {
                    ruleStr.substring(4, ruleStr.lastIndexOf("<"))
                } else {
                    ruleStr.substring(4)
                }
            } else {
                when {
                    ruleStr.startsWith("@XPath:", true) -> {
                        mode = Mode.XPath
                        rule = ruleStr.substring(7)
                    }
                    ruleStr.startsWith("//") -> {//XPath特征很明显,无需配置单独的识别标头
                        mode = Mode.XPath
                        rule = ruleStr
                    }
                    ruleStr.startsWith("@Json:", true) -> {
                        mode = Mode.Json
                        rule = ruleStr.substring(6)
                    }
                    ruleStr.startsWith("$.") -> {
                        mode = Mode.Json
                        rule = ruleStr
                    }
                    else -> rule = ruleStr
                }
            }
            //分离正则表达式
            val ruleStrS =
                rule.trim { it <= ' ' }.split("##")
            rule = ruleStrS[0]
            if (ruleStrS.size > 1) {
                replaceRegex = ruleStrS[1]
            }
            if (ruleStrS.size > 2) {
                replacement = ruleStrS[2]
            }
            if (ruleStrS.size > 3) {
                replaceFirst = true
            }
            //分离put
            rule = splitPutRule(rule, putMap)
            //@get,{{ }},$1, 拆分
            var start = 0
            var tmp: String
            val evalMatcher = evalPattern.matcher(rule)
            while (evalMatcher.find()) {
                if (evalMatcher.start() > start) {
                    tmp = rule.substring(start, evalMatcher.start())
                    ruleType.add(0)
                    ruleParam.add(tmp)
                }
                tmp = evalMatcher.group()
                when {
                    tmp.startsWith("$") -> {
                        ruleType.add(tmp.substring(1).toInt())
                        ruleParam.add(tmp)
                    }
                    tmp.startsWith("@get:", true) -> {
                        ruleType.add(-2)
                        ruleParam.add(tmp.substring(6, tmp.lastIndex))
                    }
                    tmp.startsWith("{{") -> {
                        ruleType.add(-1)
                        ruleParam.add(tmp.substring(2, tmp.length - 2))
                    }
                    else -> {
                        ruleType.add(0)
                        ruleParam.add(tmp)
                    }
                }
                start = evalMatcher.end()
            }
            if (rule.length > start) {
                tmp = rule.substring(start)
                ruleType.add(0)
                ruleParam.add(tmp)
            }
        }

        /**
         * 替换@get,{{ }},$1,
         */
        fun makeUpRule(result: Any?) {
            val infoVal = StringBuilder()
            if (ruleParam.isNotEmpty()) {
                var index = ruleParam.size
                while (index-- > 0) {
                    val regType = ruleType[index]
                    when {
                        regType > 0 -> {
                            @Suppress("UNCHECKED_CAST")
                            val resultList = result as? List<String?>
                            if (resultList != null) {
                                if (resultList.size > regType) {
                                    resultList[regType]?.let {
                                        infoVal.insert(0, resultList[regType])
                                    }
                                }
                            } else {
                                infoVal.insert(0, ruleParam[index])
                            }
                        }
                        regType == -1 -> {
                            val jsEval: Any = evalJS(ruleParam[index], result)
                            if (jsEval is String) {
                                infoVal.insert(0, jsEval)
                            } else if (jsEval is Double && jsEval % 1.0 == 0.0) {
                                infoVal.insert(0, String.format("%.0f", jsEval))
                            } else {
                                infoVal.insert(0, jsEval.toString())
                            }
                        }
                        regType == -2 -> {
                            infoVal.insert(0, get(ruleParam[index]))
                        }
                        else -> infoVal.insert(0, ruleParam[index])
                    }
                }
                rule = infoVal.toString()
            }
        }
    }

    enum class Mode {
        XPath, Json, Default, Js, Regex
    }

    fun put(key: String, value: String): String {
        book?.putVariable(key, value)
        return value
    }

    operator fun get(key: String): String {
        return book?.variableMap?.get(key) ?: ""
    }

    /**
     * 执行JS
     */
    @Throws(Exception::class)
    private fun evalJS(jsStr: String, result: Any?): Any {
        val bindings = SimpleBindings()
        bindings["java"] = this
        bindings["book"] = book
        bindings["result"] = result
        bindings["baseUrl"] = baseUrl
        return SCRIPT_ENGINE.eval(jsStr, bindings)
    }

    /**
     * js实现跨域访问,不能删
     */
    fun ajax(urlStr: String): String? {
        return try {
            val analyzeUrl = AnalyzeUrl(urlStr, null, null, null, baseUrl, book)
            val call = analyzeUrl.getResponse()
            val response = call.execute()
            response.body()
        } catch (e: Exception) {
            e.localizedMessage
        }
    }

    /**
     * js实现解码,不能删
     */
    fun base64Decoder(str: String): String {
        return EncoderUtils.base64Decoder(str)
    }

    /**
     * 章节数转数字
     */
    fun toNumChapter(s: String?): String? {
        if (s == null) {
            return null
        }
        val pattern = Pattern.compile("(第)(.+?)(章)")
        val matcher = pattern.matcher(s)
        return if (matcher.find()) {
            matcher.group(1) + StringUtils.stringToInt(matcher.group(2)) + matcher.group(3)
        } else {
            s
        }
    }

    fun strToMd5By32(str: String?): String? {
        return MD5Utils.strToMd5By32(str)
    }

    fun strToMd5By16(str: String?): String? {
        return MD5Utils.strToMd5By16(str)
    }

    companion object {
        private val putPattern = Pattern.compile("@put:(\\{[^}]+?\\})", Pattern.CASE_INSENSITIVE)
        private val getPattern = Pattern.compile("@get:\\{([^}]+?)\\}", Pattern.CASE_INSENSITIVE)
        private val evalPattern = Pattern.compile(
            "@get:\\{[^}]+?\\}|\\{\\{[\\w\\W]*?\\}\\}|\\$\\d{1,2}",
            Pattern.CASE_INSENSITIVE
        )
    }

}
