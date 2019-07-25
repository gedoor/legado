package io.legado.app.model.analyzeRule

import android.annotation.SuppressLint
import android.text.TextUtils
import androidx.annotation.Keep
import io.legado.app.constant.AppConst.SCRIPT_ENGINE
import io.legado.app.constant.Pattern.EXP_PATTERN
import io.legado.app.constant.Pattern.JS_PATTERN
import io.legado.app.data.entities.BaseBook
import io.legado.app.utils.*
import java.util.*
import java.util.regex.Pattern
import javax.script.SimpleBindings


/**
 * Created by REFGD.
 * 统一解析接口
 */
@Keep
class AnalyzeRule(private var book: BaseBook? = null) {
    private var content: Any? = null
    private var isJSON: Boolean = false
    private var baseUrl: String? = null

    private var analyzeByXPath: AnalyzeByXPath? = null
    private var analyzeByJSoup: AnalyzeByJSoup? = null
    private var analyzeByJSonPath: AnalyzeByJSonPath? = null

    private var objectChangedXP = false
    private var objectChangedJS = false
    private var objectChangedJP = false

    fun setBook(book: BaseBook) {
        this.book = book
    }

    @JvmOverloads
    fun setContent(content: Any?, baseUrl: String? = this.baseUrl): AnalyzeRule {
        if (content == null) throw AssertionError("Content cannot be null")
        isJSON = content.toString().isJson()
        this.content = content
        this.baseUrl = baseUrl?.split("\n".toRegex(), 1)?.get(0)
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
            analyzeByXPath!!.parse(content!!)
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
            analyzeByJSoup!!.parse(content!!)
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
            analyzeByJSonPath!!.parse(content!!)
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
                for (rule in ruleList) {
                    result?.let {
                        if (rule.rule.isNotEmpty()) {
                            when (rule.mode) {
                                Mode.Js -> result = evalJS(rule.rule, result)
                                Mode.JSon -> result =
                                    getAnalyzeByJSonPath(it).getStringList(rule.rule)
                                Mode.XPath -> result =
                                    getAnalyzeByXPath(it).getStringList(rule.rule)
                                else -> result = getAnalyzeByJSoup(it).getStringList(rule.rule)
                            }
                        }
                        if (rule.replaceRegex.isNotEmpty() && result is List<*>) {
                            val newList = ArrayList<String>()
                            for (item in result as List<*>) {
                                newList.add(replaceRegex(item.toString(), rule))
                            }
                            result = newList
                        } else if (rule.replaceRegex.isNotEmpty()) {
                            result = replaceRegex(result.toString(), rule)
                        }
                    }
                }
            }
        }
        if (result == null) return ArrayList()
        if (result is String) {
            result = listOf((result as String).htmlFormat().split("\n"))
        }
        baseUrl?.let {
            if (isUrl && !TextUtils.isEmpty(it)) {
                val urlList = ArrayList<String>()
                if (result is List<*>) {
                    for (url in result as List<*>) {
                        val absoluteURL = NetworkUtils.getAbsoluteURL(it, url.toString())
                        if (!urlList.contains(absoluteURL)) {
                            urlList.add(absoluteURL)
                        }
                    }
                }
                return urlList
            }
        }
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
        content?.let { o ->
            if (ruleList.isNotEmpty()) result = o
            for (rule in ruleList) {
                result?.let {
                    if (rule.rule.isNotBlank()) {
                        when (rule.mode) {
                            Mode.Js -> result = evalJS(rule.rule, it)
                            Mode.JSon -> result = getAnalyzeByJSonPath(it).getString(rule.rule)
                            Mode.XPath -> result = getAnalyzeByXPath(it).getString(rule.rule)
                            Mode.Default -> result = if (isUrl && !TextUtils.isEmpty(baseUrl)) {
                                getAnalyzeByJSoup(it).getString0(rule.rule)
                            } else {
                                getAnalyzeByJSoup(it).getString(rule.rule)
                            }
                        }
                    }
                    if (rule.replaceRegex.isNotEmpty()) {
                        result = replaceRegex(result.toString(), rule)
                    }
                }
            }
        }
        if (result == null) return ""
        baseUrl?.let {
            return if (isUrl) {
                NetworkUtils.getAbsoluteURL(it, result.toString())
            } else result.toString()
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
            for (rule in ruleList) {
                result?.let {
                    when (rule.mode) {
                        Mode.Js -> result = evalJS(rule.rule, it)
                        Mode.JSon -> result = getAnalyzeByJSonPath(it).getObject(rule.rule)
                        Mode.XPath -> result = getAnalyzeByXPath(it).getElements(rule.rule)
                        else -> result = getAnalyzeByJSoup(it).getElements(rule.rule)
                    }
                    if (rule.replaceRegex.isNotEmpty()) {
                        result = replaceRegex(result.toString(), rule)
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
            for (rule in ruleList) {
                result?.let {
                    when (rule.mode) {
                        Mode.Js -> {
                            if (result == null) result = content
                            result = evalJS(rule.rule, result)
                        }
                        Mode.JSon -> result = getAnalyzeByJSonPath(it).getList(rule.rule)
                        Mode.XPath -> result = getAnalyzeByXPath(it).getElements(rule.rule)
                        else -> result = getAnalyzeByJSoup(it).getElements(rule.rule)
                    }
                    if (rule.replaceRegex.isNotEmpty()) {
                        result = replaceRegex(result.toString(), rule)
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
     * 分离并执行put规则
     */
    @Throws(Exception::class)
    private fun splitPutRule(ruleStr: String): String {
        var vRuleStr = ruleStr
        val putMatcher = putPattern.matcher(vRuleStr)
        while (putMatcher.find()) {
            vRuleStr = vRuleStr.replace(putMatcher.group(), "")
            val map = GSON.fromJsonObject<Map<String, String>>(putMatcher.group(1))
            map?.let { putRule(map) }
        }
        return vRuleStr
    }

    /**
     * 替换@get
     */
    fun replaceGet(ruleStr: String): String {
        var vRuleStr = ruleStr
        val getMatcher = getPattern.matcher(vRuleStr)
        while (getMatcher.find()) {
            var value = ""
            book?.variableMap?.get(getMatcher.group(1))?.let {
                value = it
            }
            vRuleStr = vRuleStr.replace(getMatcher.group(), value)
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
     * 替换JS
     */
    @SuppressLint("DefaultLocale")
    @Throws(Exception::class)
    private fun replaceJs(ruleStr: String): String {
        var vRuleStr = ruleStr
        if (vRuleStr.contains("{{") && vRuleStr.contains("}}")) {
            var jsEval: Any
            val sb = StringBuffer(vRuleStr.length)
            val expMatcher = EXP_PATTERN.matcher(vRuleStr)
            while (expMatcher.find()) {
                jsEval = evalJS(expMatcher.group(1), content)
                if (jsEval is String) {
                    expMatcher.appendReplacement(sb, jsEval)
                } else if (jsEval is Double && jsEval % 1.0 == 0.0) {
                    expMatcher.appendReplacement(sb, String.format("%.0f", jsEval))
                } else {
                    expMatcher.appendReplacement(sb, jsEval.toString())
                }
            }
            expMatcher.appendTail(sb)
            vRuleStr = sb.toString()
        }
        return vRuleStr
    }

    /**
     * 分解规则生成规则列表
     */
    @Throws(Exception::class)
    fun splitSourceRule(ruleStr: String): List<SourceRule> {
        var vRuleStr = ruleStr
        val ruleList = ArrayList<SourceRule>()
        if (TextUtils.isEmpty(vRuleStr)) return ruleList
        //检测Mode
        val mode: Mode
        when {
            vRuleStr.startsWith("@XPath:", true) -> {
                mode = Mode.XPath
                vRuleStr = vRuleStr.substring(7)
            }
            vRuleStr.startsWith("@JSon:", true) -> {
                mode = Mode.JSon
                vRuleStr = vRuleStr.substring(6)
            }
            else -> mode = if (isJSON) {
                Mode.JSon
            } else {
                Mode.Default
            }
        }
        //分离put规则
        vRuleStr = splitPutRule(vRuleStr)
        //替换get值
        vRuleStr = replaceGet(vRuleStr)
        //替换js
        vRuleStr = replaceJs(vRuleStr)
        //拆分为列表
        var start = 0
        var tmp: String
        val jsMatcher = JS_PATTERN.matcher(vRuleStr)
        while (jsMatcher.find()) {
            if (jsMatcher.start() > start) {
                tmp = vRuleStr.substring(start, jsMatcher.start()).replace("\n".toRegex(), "").trim { it <= ' ' }
                if (!TextUtils.isEmpty(tmp)) {
                    ruleList.add(SourceRule(tmp, mode))
                }
            }
            ruleList.add(SourceRule(jsMatcher.group(), Mode.Js))
            start = jsMatcher.end()
        }
        if (vRuleStr.length > start) {
            tmp = vRuleStr.substring(start).replace("\n".toRegex(), "").trim { it <= ' ' }
            if (!TextUtils.isEmpty(tmp)) {
                ruleList.add(SourceRule(tmp, mode))
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
                    ruleStr.startsWith("@JSon:", true) -> {
                        mode = Mode.JSon
                        rule = ruleStr.substring(6)
                    }
                    ruleStr.startsWith("$.") -> {
                        mode = Mode.JSon
                        rule = ruleStr
                    }
                    else -> rule = ruleStr
                }
            }
            //分离正则表达式
            val ruleStrS =
                rule.trim { it <= ' ' }.splitNotBlank("##")
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
        }

    }

    enum class Mode {
        XPath, JSon, Default, Js
    }

    fun put(key: String, value: String): String {
        if (book != null) {
            book!!.putVariable(key, value)
        }
        return value
    }

    operator fun get(key: String): String? {
        return book?.variableMap?.get(key)
    }

    /**
     * 执行JS
     */
    @Throws(Exception::class)
    private fun evalJS(jsStr: String, result: Any?): Any {
        val bindings = SimpleBindings()
        bindings["java"] = this
        bindings["result"] = result
        bindings["baseUrl"] = baseUrl
        return SCRIPT_ENGINE.eval(jsStr, bindings)
    }

    /**
     * js实现跨域访问,不能删
     */
//    fun ajax(urlStr: String): String? {
//        try {
//            val analyzeUrl = AnalyzeUrl(urlStr)
//            val response = BaseModelImpl.getInstance().getResponseO(analyzeUrl)
//                .blockingFirst()
//            return response.body()
//        } catch (e: Exception) {
//            return e.localizedMessage
//        }
//
//    }

    /**
     * js实现解码,不能删
     */
//    fun base64Decoder(base64: String): String {
//        return StringUtils.base64Decode(base64)
//    }

    /**
     * 章节数转数字
     */
//    fun toNumChapter(s: String?): String? {
//        if (s == null) {
//            return null
//        }
//        val pattern = Pattern.compile("(第)(.+?)(章)")
//        val matcher = pattern.matcher(s)
//        return if (matcher.find()) {
//            matcher.group(1) + StringUtils.stringToInt(matcher.group(2)) + matcher.group(3)
//        } else s
//    }

    companion object {
        private val putPattern = Pattern.compile("@put:(\\{[^}]+?\\})", Pattern.CASE_INSENSITIVE)
        private val getPattern = Pattern.compile("@get:\\{([^}]+?)\\}", Pattern.CASE_INSENSITIVE)
    }

}
