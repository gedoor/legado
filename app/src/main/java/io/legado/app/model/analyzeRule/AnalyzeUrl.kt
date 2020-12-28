package io.legado.app.model.analyzeRule

import android.annotation.SuppressLint
import android.text.TextUtils
import androidx.annotation.Keep
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import io.legado.app.constant.AppConst.SCRIPT_ENGINE
import io.legado.app.constant.AppConst.UA_NAME
import io.legado.app.constant.AppPattern.EXP_PATTERN
import io.legado.app.constant.AppPattern.JS_PATTERN
import io.legado.app.data.entities.BaseBook
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.AppConfig
import io.legado.app.help.CacheManager
import io.legado.app.help.JsExtensions
import io.legado.app.help.http.*
import io.legado.app.utils.*
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toByteArray
import rxhttp.wrapper.param.toStrResponse
import java.net.URLEncoder
import java.util.*
import java.util.regex.Pattern
import javax.script.SimpleBindings

/**
 * Created by GKF on 2018/1/24.
 * 搜索URL规则解析
 */
@Keep
@SuppressLint("DefaultLocale")
class AnalyzeUrl(
    var ruleUrl: String,
    val key: String? = null,
    val page: Int? = null,
    val speakText: String? = null,
    val speakSpeed: Int? = null,
    var baseUrl: String = "",
    var useWebView: Boolean = false,
    val book: BaseBook? = null,
    val chapter: BookChapter? = null,
    private val ruleData: RuleDataInterface? = null,
    headerMapF: Map<String, String>? = null
) : JsExtensions {
    companion object {
        val splitUrlRegex = Regex(",\\s*(?=\\{)")
        private val pagePattern = Pattern.compile("<(.*?)>")
    }

    var url: String = ""
    val headerMap = HashMap<String, String>()
    var body: String? = null
    var type: String? = null
    private lateinit var urlHasQuery: String
    private var queryStr: String? = null
    private val fieldMap = LinkedHashMap<String, String>()
    private var charset: String? = null
    private var method = RequestMethod.GET
    private var proxy: String? = null

    init {
        baseUrl = baseUrl.split(splitUrlRegex, 1)[0]
        headerMapF?.let {
            headerMap.putAll(it)
            if (it.containsKey("proxy")) {
                proxy = it["proxy"]
                headerMap.remove("proxy")
            }
        }
        //替换参数
        analyzeJs()
        replaceKeyPageJs()
        //处理URL
        initUrl()
    }

    private fun analyzeJs() {
        val ruleList = arrayListOf<String>()
        var start = 0
        var tmp: String
        val jsMatcher = JS_PATTERN.matcher(ruleUrl)
        while (jsMatcher.find()) {
            if (jsMatcher.start() > start) {
                tmp =
                    ruleUrl.substring(start, jsMatcher.start()).replace("\n", "").trim { it <= ' ' }
                if (!TextUtils.isEmpty(tmp)) {
                    ruleList.add(tmp)
                }
            }
            ruleList.add(jsMatcher.group())
            start = jsMatcher.end()
        }
        if (ruleUrl.length > start) {
            tmp = ruleUrl.substring(start).replace("\n", "").trim { it <= ' ' }
            if (!TextUtils.isEmpty(tmp)) {
                ruleList.add(tmp)
            }
        }
        for (rule in ruleList) {
            var ruleStr = rule
            when {
                ruleStr.startsWith("<js>") -> {
                    ruleStr = ruleStr.substring(4, ruleStr.lastIndexOf("<"))
                    ruleUrl = evalJS(ruleStr, ruleUrl) as String
                }
                ruleStr.startsWith("@js", true) -> {
                    ruleStr = ruleStr.substring(4)
                    ruleUrl = evalJS(ruleStr, ruleUrl) as String
                }
                else -> ruleUrl = ruleStr.replace("@result", ruleUrl)
            }
        }
    }

    /**
     * 替换关键字,页数,JS
     */
    private fun replaceKeyPageJs() {
        //page
        page?.let {
            val matcher = pagePattern.matcher(ruleUrl)
            while (matcher.find()) {
                val pages = matcher.group(1)!!.split(",")
                ruleUrl = if (page <= pages.size) {
                    ruleUrl.replace(matcher.group(), pages[page - 1].trim { it <= ' ' })
                } else {
                    ruleUrl.replace(matcher.group(), pages.last().trim { it <= ' ' })
                }
            }
        }
        //js
        if (ruleUrl.contains("{{") && ruleUrl.contains("}}")) {
            var jsEval: Any
            val sb = StringBuffer()
            val bindings = SimpleBindings()
            bindings["java"] = this
            bindings["cookie"] = CookieStore
            bindings["cache"] = CacheManager
            bindings["baseUrl"] = baseUrl
            bindings["page"] = page
            bindings["key"] = key
            bindings["speakText"] = speakText
            bindings["speakSpeed"] = speakSpeed
            bindings["book"] = book
            val expMatcher = EXP_PATTERN.matcher(ruleUrl)
            while (expMatcher.find()) {
                jsEval = expMatcher.group(1)?.let {
                    SCRIPT_ENGINE.eval(it, bindings)
                } ?: ""
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
    }

    /**
     * 处理URL
     */
    private fun initUrl() {
        var urlArray = ruleUrl.split(splitUrlRegex, 2)
        url = NetworkUtils.getAbsoluteURL(baseUrl, urlArray[0])
        urlHasQuery = urlArray[0]
        NetworkUtils.getBaseUrl(url)?.let {
            baseUrl = it
        }
        if (urlArray.size > 1) {
            val option = GSON.fromJsonObject<UrlOption>(urlArray[1])
            option?.let { _ ->
                option.method?.let {
                    if (it.equals("POST", true)) method = RequestMethod.POST
                }
                option.type?.let { type = it }
                option.headers?.let { headers ->
                    if (headers is Map<*, *>) {
                        headers.forEach { entry ->
                            headerMap[entry.key.toString()] = entry.value.toString()
                        }
                    } else if (headers is String) {
                        GSON.fromJsonObject<Map<String, String>>(headers)
                            ?.let { headerMap.putAll(it) }
                    }
                }
                option.charset?.let { charset = it }
                option.body?.let {
                    body = if (it is String) it else GSON.toJson(it)
                }
                option.webView?.let {
                    if (it.toString().isNotEmpty()) {
                        useWebView = true
                    }
                }
                option.js?.let {
                    evalJS(it)
                }
            }
        }
        headerMap[UA_NAME] ?: let {
            headerMap[UA_NAME] = AppConfig.userAgent
        }
        when (method) {
            RequestMethod.GET -> {
                if (!useWebView) {
                    urlArray = url.split("?")
                    url = urlArray[0]
                    if (urlArray.size > 1) {
                        analyzeFields(urlArray[1])
                    }
                }
            }
            RequestMethod.POST -> {
                body?.let {
                    if (!it.isJson()) {
                        analyzeFields(it)
                    }
                }
            }
        }
    }

    /**
     * 解析QueryMap
     */
    private fun analyzeFields(fieldsTxt: String) {
        queryStr = fieldsTxt
        val queryS = fieldsTxt.splitNotBlank("&")
        for (query in queryS) {
            val queryM = query.splitNotBlank("=")
            val value = if (queryM.size > 1) queryM[1] else ""
            if (TextUtils.isEmpty(charset)) {
                if (NetworkUtils.hasUrlEncoded(value)) {
                    fieldMap[queryM[0]] = value
                } else {
                    fieldMap[queryM[0]] = URLEncoder.encode(value, "UTF-8")
                }
            } else if (charset == "escape") {
                fieldMap[queryM[0]] = EncoderUtils.escape(value)
            } else {
                fieldMap[queryM[0]] = URLEncoder.encode(value, charset)
            }
        }
    }

    /**
     * 执行JS
     */
    private fun evalJS(jsStr: String, result: Any? = null): Any? {
        val bindings = SimpleBindings()
        bindings["java"] = this
        bindings["cookie"] = CookieStore
        bindings["cache"] = CacheManager
        bindings["page"] = page
        bindings["key"] = key
        bindings["speakText"] = speakText
        bindings["speakSpeed"] = speakSpeed
        bindings["book"] = book
        bindings["result"] = result
        bindings["baseUrl"] = baseUrl
        return SCRIPT_ENGINE.eval(jsStr, bindings)
    }

    fun put(key: String, value: String): String {
        chapter?.putVariable(key, value)
            ?: book?.putVariable(key, value)
            ?: ruleData?.putVariable(key, value)
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
            ?: ruleData?.variableMap?.get(key)
            ?: ""
    }

    suspend fun getStrResponse(
        tag: String,
        jsStr: String? = null,
        sourceRegex: String? = null,
    ): StrResponse {
        if (type != null) {
            return StrResponse(url, StringUtils.byteToHexString(getByteArray(tag)))
        }
        setCookie(tag)
        if (useWebView) {
            val params = AjaxWebView.AjaxParams(url)
            params.headerMap = headerMap
            params.requestMethod = method
            params.javaScript = jsStr
            params.sourceRegex = sourceRegex
            params.postData = body?.toByteArray()
            params.tag = tag
            return HttpHelper.ajax(params)
        }
        return when (method) {
            RequestMethod.POST -> {
                if (fieldMap.isNotEmpty()) {
                    RxHttp.postForm(url)
                        .setAssemblyEnabled(false)
                        .setOkClient(HttpHelper.getProxyClient(proxy))
                        .addAllEncoded(fieldMap)
                        .addAllHeader(headerMap)
                        .toStrResponse().await()
                } else {
                    RxHttp.postJson(url)
                        .setAssemblyEnabled(false)
                        .setOkClient(HttpHelper.getProxyClient(proxy))
                        .addAll(body)
                        .addAllHeader(headerMap)
                        .toStrResponse().await()
                }
            }
            else -> RxHttp.get(url)
                .setAssemblyEnabled(false)
                .setOkClient(HttpHelper.getProxyClient(proxy))
                .addAllEncoded(fieldMap)
                .addAllHeader(headerMap)
                .toStrResponse().await()
        }
    }

    suspend fun getByteArray(tag: String? = null): ByteArray {
        setCookie(tag)
        return when (method) {
            RequestMethod.POST -> {
                if (fieldMap.isNotEmpty()) {
                    RxHttp.postForm(url)
                        .setAssemblyEnabled(false)
                        .setOkClient(HttpHelper.getProxyClient(proxy))
                        .addAllEncoded(fieldMap)
                        .addAllHeader(headerMap)
                        .toByteArray().await()
                } else {
                    RxHttp.postJson(url)
                        .setAssemblyEnabled(false)
                        .setOkClient(HttpHelper.getProxyClient(proxy))
                        .addAll(body)
                        .addAllHeader(headerMap)
                        .toByteArray().await()
                }
            }
            else -> RxHttp.get(url)
                .setAssemblyEnabled(false)
                .setOkClient(HttpHelper.getProxyClient(proxy))
                .addAllEncoded(fieldMap)
                .addAllHeader(headerMap)
                .toByteArray().await()
        }
    }

    private fun setCookie(tag: String?) {
        if (tag != null) {
            val cookie = CookieStore.getCookie(tag)
            if (cookie.isNotEmpty()) {
                val cookieMap = CookieStore.cookieToMap(cookie)
                val customCookieMap = CookieStore.cookieToMap(headerMap["Cookie"] ?: "")
                cookieMap.putAll(customCookieMap)
                val newCookie = CookieStore.mapToCookie(cookieMap)
                newCookie?.let {
                    headerMap.put("Cookie", it)
                }
            }
        }
    }

    fun getGlideUrl(): GlideUrl {
        val headers = LazyHeaders.Builder()
        headerMap.forEach { (key, value) ->
            headers.addHeader(key, value)
        }
        return GlideUrl(urlHasQuery, headers.build())
    }

    data class UrlOption(
        val method: String?,
        val charset: String?,
        val webView: Any?,
        val headers: Any?,
        val body: Any?,
        val type: String?,
        val js: String?
    )

}
