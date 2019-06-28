package io.legado.app.model.analyzeRule

import android.annotation.SuppressLint
import android.text.TextUtils
import androidx.annotation.Keep
import io.legado.app.constant.AppConst.SCRIPT_ENGINE
import io.legado.app.constant.Pattern.EXP_PATTERN
import io.legado.app.constant.Pattern.JS_PATTERN
import io.legado.app.utils.NetworkUtils
import java.net.URLEncoder
import java.util.*
import java.util.regex.Pattern
import javax.script.SimpleBindings


/**
 * Created by GKF on 2018/1/24.
 * 搜索URL规则解析
 */
@Keep
class AnalyzeUrl @SuppressLint("DefaultLocale")
@Throws(Exception::class)
constructor(ruleUrl: String, key: String?, page: Int?, headerMapF: Map<String, String>?, baseUrl: String?) {
    private var baseUrl: String? = null
    var url: String? = null
        private set
    var host: String? = null
        private set
    var path: String? = null
        private set
    var queryStr: String? = null
        private set
    private val queryMap = LinkedHashMap<String, String>()
    private val headerMap = HashMap<String, String>()
    private var charCode: String? = null
    var urlMode = UrlMode.DEFAULT
        private set

    val postData: ByteArray
        get() {
            val builder = StringBuilder()
            val keys = queryMap.keys
            for (key in keys) {
                builder.append(String.format("%s=%s&", key, queryMap[key]))
            }
            builder.deleteCharAt(builder.lastIndexOf("&"))
            return builder.toString().toByteArray()
        }

    @Throws(Exception::class)
    constructor(urlRule: String) : this(urlRule, null, null, null, null)

    @Throws(Exception::class)
    constructor(urlRule: String, headerMapF: Map<String, String>, baseUrl: String) : this(
        urlRule,
        null,
        null,
        headerMapF,
        baseUrl
    )

    init {
        var ruleUrl = ruleUrl
        if (!TextUtils.isEmpty(baseUrl)) {
//            this.baseUrl = headerPattern.matcher(baseUrl).replaceAll("")
        }
        //解析Header
        ruleUrl = analyzeHeader(ruleUrl, headerMapF)
        //替换关键字
        key?.let {
            if (it.isNotBlank()) {
                ruleUrl = ruleUrl.replace("searchKey", it)
            }
        }
        //分离编码规则
        ruleUrl = splitCharCode(ruleUrl)
        //判断是否有下一页
        if (page != null && page > 1 && !ruleUrl.contains("searchPage"))
            throw Exception("没有下一页")
        //替换js
        ruleUrl = replaceJs(ruleUrl, baseUrl, page, key)
        //设置页数
        ruleUrl = analyzePage(ruleUrl, page)
        //执行规则列表
        val ruleList = splitRule(ruleUrl)
        for (rule in ruleList) {
            ruleUrl = if (rule.startsWith("<js>")) {
                evalJS(rule.substring(4, rule.lastIndexOf("<")), ruleUrl) as String
            } else {
                rule.replace("@result", ruleUrl)
            }
        }
        //分离post参数
        var ruleUrlS = ruleUrl.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (ruleUrlS.size > 1) {
            urlMode = UrlMode.POST
        } else {
            //分离get参数
            ruleUrlS = ruleUrlS[0].split("\\?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (ruleUrlS.size > 1) {
                urlMode = UrlMode.GET
            }
        }
        generateUrlPath(ruleUrlS[0])
        if (urlMode != UrlMode.DEFAULT) {
            analyzeQuery(ruleUrlS[1])
        }
    }

    /**
     * 解析Header
     */
    private fun analyzeHeader(ruleUrl: String, headerMapF: Map<String, String>?): String {
//        var ruleUrl = ruleUrl
//        if (headerMapF != null) {
//            headerMap.putAll(headerMapF)
//        }
//        val matcher = headerPattern.matcher(ruleUrl)
//        if (matcher.find()) {
//            var find = matcher.group(0)
//            ruleUrl = ruleUrl.replace(find, "")
//            find = find.substring(8)
//            try {
//                val map = Gson().fromJsonObject<Map<String, String>>(find)
//                headerMap.putAll(map)
//            } catch (ignored: Exception) {
//            }
//        }
        return ruleUrl
    }

    /**
     * 分离编码规则
     */
    private fun splitCharCode(rule: String): String {
        val ruleUrlS = rule.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (ruleUrlS.size > 1) {
            if (!TextUtils.isEmpty(ruleUrlS[1])) {
                val qtS = ruleUrlS[1].split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (qt in qtS) {
                    val gz = qt.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (gz[0] == "char") {
                        charCode = gz[1]
                    }
                }
            }
        }
        return ruleUrlS[0]
    }

    /**
     * 解析页数
     */
    private fun analyzePage(ruleUrl: String, searchPage: Int?): String {
        var ruleUrl = ruleUrl
        if (searchPage == null) return ruleUrl
        val matcher = pagePattern.matcher(ruleUrl)
        while (matcher.find()) {
            val pages = matcher.group(1).split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (searchPage <= pages.size) {
                ruleUrl = ruleUrl.replace(matcher.group(), pages[searchPage - 1].trim { it <= ' ' })
            } else {
                ruleUrl = ruleUrl.replace(matcher.group(), pages[pages.size - 1].trim { it <= ' ' })
            }
        }
        return ruleUrl.replace("searchPage-1", (searchPage - 1).toString())
            .replace("searchPage+1", (searchPage + 1).toString())
            .replace("searchPage", searchPage.toString())
    }

    /**
     * 替换js
     */
    @SuppressLint("DefaultLocale")
    @Throws(Exception::class)
    private fun replaceJs(ruleUrl: String, baseUrl: String?, searchPage: Int?, searchKey: String?): String {
        var ruleUrl = ruleUrl
        if (ruleUrl.contains("{{") && ruleUrl.contains("}}")) {
            var jsEval: Any
            val sb = StringBuffer(ruleUrl.length)
            val simpleBindings = object : SimpleBindings() {
                init {
                    this["baseUrl"] = baseUrl
                    this["searchPage"] = searchPage
                    this["searchKey"] = searchKey
                }
            }
            val expMatcher = EXP_PATTERN.matcher(ruleUrl)
            while (expMatcher.find()) {
                jsEval = SCRIPT_ENGINE.eval(expMatcher.group(1), simpleBindings)
                if (jsEval is String) {
                    expMatcher.appendReplacement(sb, jsEval)
                } else if (jsEval is Double && jsEval % 1.0 == 0.0) {
                    expMatcher.appendReplacement(sb, String.format("%.0f", jsEval))
                } else {
                    expMatcher.appendReplacement(sb, jsEval.toString())
                }
            }
            expMatcher.appendTail(sb)
            ruleUrl = sb.toString()
        }
        return ruleUrl
    }

    /**
     * 解析QueryMap
     */
    @Throws(Exception::class)
    private fun analyzeQuery(allQuery: String) {
        queryStr = allQuery
        val queryS = allQuery.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (query in queryS) {
            val queryM = query.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val value = if (queryM.size > 1) queryM[1] else ""
            if (TextUtils.isEmpty(charCode)) {
                if (NetworkUtils.hasUrlEncoded(value)) {
                    queryMap[queryM[0]] = value
                } else {
                    queryMap[queryM[0]] = URLEncoder.encode(value, "UTF-8")
                }
            } else if (charCode == "escape") {
//                queryMap[queryM[0]] = StringUtils.escape(value)
            } else {
                queryMap[queryM[0]] = URLEncoder.encode(value, charCode)
            }
        }
    }

    /**
     * 拆分规则
     */
    private fun splitRule(ruleStr: String): List<String> {
        val ruleList = ArrayList<String>()
        val jsMatcher = JS_PATTERN.matcher(ruleStr)
        var start = 0
        var tmp: String
        while (jsMatcher.find()) {
            if (jsMatcher.start() > start) {
                tmp = ruleStr.substring(start, jsMatcher.start()).replace("\n".toRegex(), "").trim { it <= ' ' }
                if (!TextUtils.isEmpty(tmp)) {
                    ruleList.add(tmp)
                }
            }
            ruleList.add(jsMatcher.group())
            start = jsMatcher.end()
        }
        if (ruleStr.length > start) {
            tmp = ruleStr.substring(start).replace("\n".toRegex(), "").trim { it <= ' ' }
            if (!TextUtils.isEmpty(tmp)) {
                ruleList.add(tmp)
            }
        }
        return ruleList
    }

    /**
     * 分解URL
     */
    private fun generateUrlPath(ruleUrl: String) {
        baseUrl?.let { url = NetworkUtils.getAbsoluteURL(it, ruleUrl) }
        host = NetworkUtils.getBaseUrl(url)
        path = url!!.substring(host!!.length)
    }

    /**
     * 执行JS
     */
    @Throws(Exception::class)
    private fun evalJS(jsStr: String, result: Any): Any {
        val bindings = SimpleBindings()
        bindings["result"] = result
        return SCRIPT_ENGINE.eval(jsStr, bindings)
    }

    fun getQueryMap(): Map<String, String> {
        return queryMap
    }

    fun getHeaderMap(): Map<String, String> {
        return headerMap
    }

    enum class UrlMode {
        GET, POST, DEFAULT
    }

    companion object {
        private val pagePattern = Pattern.compile("\\{(.*?)\\}")
    }
}
