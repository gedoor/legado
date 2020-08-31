package io.legado.app.model.analyzeRule

import android.annotation.SuppressLint
import android.text.TextUtils
import androidx.annotation.Keep
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import io.legado.app.constant.AppConst.SCRIPT_ENGINE
import io.legado.app.constant.AppConst.UA_NAME
import io.legado.app.constant.AppConst.userAgent
import io.legado.app.constant.AppPattern.EXP_PATTERN
import io.legado.app.constant.AppPattern.JS_PATTERN
import io.legado.app.data.entities.BaseBook
import io.legado.app.help.JsExtensions
import io.legado.app.help.http.*
import io.legado.app.help.http.api.HttpGetApi
import io.legado.app.help.http.api.HttpPostApi
import io.legado.app.utils.*
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Call
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
    key: String? = null,
    page: Int? = null,
    speakText: String? = null,
    speakSpeed: Int? = null,
    headerMapF: Map<String, String>? = null,
    baseUrl: String? = null,
    book: BaseBook? = null,
    var useWebView: Boolean = false,
) : JsExtensions {
    companion object {
        private val pagePattern = Pattern.compile("<(.*?)>")
        private val jsonType = MediaType.parse("application/json; charset=utf-8")
    }

    private var baseUrl: String = ""
    lateinit var url: String
        private set
    private lateinit var urlHasQuery: String
    val headerMap = HashMap<String, String>()
    private var queryStr: String? = null
    private val fieldMap = LinkedHashMap<String, String>()
    private var charset: String? = null
    private var body: String? = null
    private var requestBody: RequestBody? = null
    private var method = RequestMethod.GET
    private val splitUrlRegex = Regex(",\\s*(?=\\{)")
    private var proxy: String? = null

    init {
        baseUrl?.let {
            this.baseUrl = it.split(splitUrlRegex, 1)[0]
        }
        headerMapF?.let {
            headerMap.putAll(it)
            if (it.containsKey("proxy")) {
                proxy = it["proxy"]
                headerMap.remove("proxy")
            }
        }
        //替换参数
        analyzeJs(key, page, speakText, speakSpeed, book)
        replaceKeyPageJs(key, page, speakText, speakSpeed, book)
        //处理URL
        initUrl()
    }

    private fun analyzeJs(
        key: String?,
        page: Int?,
        speakText: String?,
        speakSpeed: Int?,
        book: BaseBook?,
    ) {
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
                    ruleUrl =
                        evalJS(ruleStr, ruleUrl, page, key, speakText, speakSpeed, book) as String
                }
                ruleStr.startsWith("@js", true) -> {
                    ruleStr = ruleStr.substring(4)
                    ruleUrl =
                        evalJS(ruleStr, ruleUrl, page, key, speakText, speakSpeed, book) as String
                }
                else -> ruleUrl = ruleStr.replace("@result", ruleUrl)
            }
        }
    }

    /**
     * 替换关键字,页数,JS
     */
    private fun replaceKeyPageJs(
        key: String?,
        page: Int?,
        speakText: String?,
        speakSpeed: Int?,
        book: BaseBook?,
    ) {
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
            val sb = StringBuffer(ruleUrl.length)
            val simpleBindings = SimpleBindings()
            simpleBindings["java"] = this
            simpleBindings["baseUrl"] = baseUrl
            simpleBindings["page"] = page
            simpleBindings["key"] = key
            simpleBindings["speakText"] = speakText
            simpleBindings["speakSpeed"] = speakSpeed
            simpleBindings["book"] = book
            val expMatcher = EXP_PATTERN.matcher(ruleUrl)
            while (expMatcher.find()) {
                jsEval = expMatcher.group(1)?.let {
                    SCRIPT_ENGINE.eval(it, simpleBindings)
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
        url = urlArray[0]
        urlHasQuery = urlArray[0]
        NetworkUtils.getBaseUrl(url)?.let {
            baseUrl = it
        }
        if (urlArray.size > 1) {
            val option = GSON.fromJsonObject<UrlOption>(urlArray[1])
            option?.let { _ ->
                option.method?.let { if (it.equals("POST", true)) method = RequestMethod.POST }
                option.headers?.let { headers ->
                    if (headers is Map<*, *>) {
                        @Suppress("unchecked_cast")
                        headerMap.putAll(headers as Map<out String, String>)
                    }
                    if (headers is String) {
                        GSON.fromJsonObject<Map<String, String>>(headers)
                            ?.let { headerMap.putAll(it) }
                    }
                }
                headerMap[UA_NAME] = headerMap[UA_NAME] ?: userAgent
                charset = option.charset
                body = if (option.body is String) {
                    option.body
                } else {
                    GSON.toJson(option.body)
                }
                option.webView?.let {
                    if (it.toString().isNotEmpty()) {
                        useWebView = true
                    }
                }
            }
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
                    if (it.isJson()) {
                        requestBody = RequestBody.create(jsonType, it)
                    } else {
                        analyzeFields(it)
                    }
                } ?: let {
                    requestBody = FormBody.Builder().build()
                }
            }
        }
    }

    /**
     * 解析QueryMap
     */
    @Throws(Exception::class)
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
    @Throws(Exception::class)
    private fun evalJS(
        jsStr: String,
        result: Any?,
        page: Int?,
        key: String?,
        speakText: String?,
        speakSpeed: Int?,
        book: BaseBook?,
    ): Any {
        val bindings = SimpleBindings()
        bindings["java"] = this
        bindings["page"] = page
        bindings["key"] = key
        bindings["speakText"] = speakText
        bindings["speakSpeed"] = speakSpeed
        bindings["book"] = book
        bindings["result"] = result
        bindings["baseUrl"] = baseUrl
        return SCRIPT_ENGINE.eval(jsStr, bindings)
    }

    @Throws(Exception::class)
    fun getResponse(tag: String): Call<String> {
        val cookie = CookieStore.getCookie(tag)
        if (cookie.isNotEmpty()) {
            headerMap["Cookie"] = cookie
        }
        return when {
            method == RequestMethod.POST -> {
                if (fieldMap.isNotEmpty()) {
                    HttpHelper
                        .getApiService<HttpPostApi>(baseUrl, charset)
                        .postMap(url, fieldMap, headerMap)
                } else {
                    HttpHelper
                        .getApiService<HttpPostApi>(baseUrl, charset)
                        .postBody(url, requestBody!!, headerMap)
                }
            }
            fieldMap.isEmpty() -> HttpHelper
                .getApiService<HttpGetApi>(baseUrl, charset)
                .get(url, headerMap)
            else -> HttpHelper
                .getApiService<HttpGetApi>(baseUrl, charset)
                .getMap(url, fieldMap, headerMap)
        }
    }

    @Throws(Exception::class)
    suspend fun getResponseAwait(
        tag: String,
        jsStr: String? = null,
        sourceRegex: String? = null,
    ): Res {
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
        val cookie = CookieStore.getCookie(tag)
        if (cookie.isNotEmpty()) {
            headerMap["Cookie"] = cookie
        }
        val res = when {
            method == RequestMethod.POST -> {
                if (fieldMap.isNotEmpty()) {
                    if (proxy == null) {
                        HttpHelper
                            .getApiService<HttpPostApi>(baseUrl, charset)
                            .postMapAsync(url, fieldMap, headerMap)
                    } else {
                        HttpHelper
                            .getApiServiceWithProxy<HttpPostApi>(baseUrl, charset, proxy)
                            .postMapAsync(url, fieldMap, headerMap)
                    }
                } else {
                    if (proxy == null) {
                        HttpHelper
                            .getApiService<HttpPostApi>(baseUrl, charset)
                            .postBodyAsync(url, requestBody!!, headerMap)
                    } else {
                        HttpHelper
                            .getApiServiceWithProxy<HttpPostApi>(baseUrl, charset, proxy)
                            .postBodyAsync(url, requestBody!!, headerMap)
                    }
                }
            }
            fieldMap.isEmpty() -> {
                if (proxy == null) {
                    HttpHelper
                        .getApiService<HttpGetApi>(baseUrl, charset)
                        .getAsync(url, headerMap)

                } else {
                    HttpHelper
                        .getApiServiceWithProxy<HttpGetApi>(baseUrl, charset, proxy)
                        .getAsync(url, headerMap)
                }

            }
            else -> {
                if (proxy == null) {
                    HttpHelper
                        .getApiService<HttpGetApi>(baseUrl, charset)
                        .getMapAsync(url, fieldMap, headerMap)
                } else {
                    HttpHelper
                        .getApiServiceWithProxy<HttpGetApi>(baseUrl, charset, proxy)
                        .getMapAsync(url, fieldMap, headerMap)
                }

            }
        }
        return Res(NetworkUtils.getUrl(res), res.body())
    }

    @Throws(Exception::class)
    fun getImageBytes(tag: String): ByteArray? {
        val cookie = CookieStore.getCookie(tag)
        if (cookie.isNotEmpty()) {
            headerMap["Cookie"] += cookie
        }
        return if (fieldMap.isEmpty()) {
            HttpHelper.getBytes(url, mapOf(), headerMap)
        } else {
            HttpHelper.getBytes(url, fieldMap, headerMap)
        }
    }

    @Throws(Exception::class)
    suspend fun getResponseBytes(tag: String? = null): ByteArray? {
        if (tag != null) {
            val cookie = CookieStore.getCookie(tag)
            if (cookie.isNotEmpty()) {
                headerMap["Cookie"] = cookie
            }
        }
        val response = when {
            method == RequestMethod.POST -> {
                if (fieldMap.isNotEmpty()) {
                    HttpHelper
                        .getBytesApiService<HttpPostApi>(baseUrl)
                        .postMapByteAsync(url, fieldMap, headerMap)
                } else {
                    HttpHelper
                        .getBytesApiService<HttpPostApi>(baseUrl)
                        .postBodyByteAsync(url, requestBody!!, headerMap)
                }
            }
            fieldMap.isEmpty() -> HttpHelper
                .getBytesApiService<HttpGetApi>(baseUrl)
                .getByteAsync(url, headerMap)
            else -> HttpHelper
                .getBytesApiService<HttpGetApi>(baseUrl)
                .getMapByteAsync(url, fieldMap, headerMap)
        }
        return response.body()
    }

    @Throws(Exception::class)
    fun getGlideUrl(): Any? {
        var glideUrl: Any = urlHasQuery
        if (headerMap.isNotEmpty()) {
            val headers = LazyHeaders.Builder()
            headerMap.forEach { (key, value) ->
                headers.addHeader(key, value)
            }
            glideUrl = GlideUrl(urlHasQuery, headers.build())
        }
        return glideUrl
    }

    data class UrlOption(
        val method: String?,
        val charset: String?,
        val webView: Any?,
        val headers: Any?,
        val body: Any?,
    )

}
