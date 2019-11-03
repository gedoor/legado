package io.legado.app.model.analyzeRule

import android.annotation.SuppressLint
import android.text.TextUtils
import androidx.annotation.Keep
import io.legado.app.constant.AppConst.SCRIPT_ENGINE
import io.legado.app.constant.Pattern.EXP_PATTERN
import io.legado.app.constant.Pattern.JS_PATTERN
import io.legado.app.data.api.IHttpGetApi
import io.legado.app.data.api.IHttpPostApi
import io.legado.app.data.entities.BaseBook
import io.legado.app.help.JsExtensions
import io.legado.app.help.http.AjaxWebView
import io.legado.app.help.http.HttpHelper
import io.legado.app.help.http.RequestMethod
import io.legado.app.utils.*
import kotlinx.coroutines.Deferred
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response
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
    headerMapF: Map<String, String>? = null,
    baseUrl: String? = null,
    book: BaseBook? = null
) {
    companion object {
        private val pagePattern = Pattern.compile("<(.*?)>")
        private val jsonType = "application/json; charset=utf-8".toMediaTypeOrNull()
    }

    private var baseUrl: String = ""
    lateinit var url: String
        private set
    var path: String? = null
        private set
    private var queryStr: String? = null
    private val fieldMap = LinkedHashMap<String, String>()
    private val headerMap = HashMap<String, String>()
    private var charset: String? = null
    private var bodyTxt: String? = null
    private var body: RequestBody? = null
    private var method = RequestMethod.GET
    private var webViewJs: String? = null
    private var sourceRegex: String? = null

    init {
        baseUrl?.let {
            this.baseUrl = it.split(",[^\\{]*".toRegex(), 1)[0]
        }
        headerMapF?.let { headerMap.putAll(it) }
        //替换参数
        analyzeJs(key, page, book)
        replaceKeyPageJs(key, page, book)
        //处理URL
        initUrl()
    }

    private fun analyzeJs(key: String?, page: Int?, book: BaseBook?) {
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
                    ruleUrl = evalJS(ruleStr, ruleUrl, page, key, book) as String
                }
                ruleStr.startsWith("@js", true) -> {
                    ruleStr = ruleStr.substring(4)
                    ruleUrl = evalJS(ruleStr, ruleUrl, page, key, book) as String
                }
                else -> ruleUrl = ruleStr.replace("@result", ruleUrl)
            }
        }
    }

    /**
     * 替换关键字,页数,JS
     */
    private fun replaceKeyPageJs(key: String?, page: Int?, book: BaseBook?) {
        //page
        page?.let {
            val matcher = pagePattern.matcher(ruleUrl)
            while (matcher.find()) {
                val pages = matcher.group(1).split(",")
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
            simpleBindings["java"] = JsExtensions
            simpleBindings["baseUrl"] = baseUrl
            simpleBindings["page"] = page
            simpleBindings["key"] = key
            simpleBindings["book"] = book
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
    }

    /**
     * 处理URL
     */
    private fun initUrl() {
        var urlArray = ruleUrl.split(",[^\\{]*".toRegex(), 2)
        url = urlArray[0]
        NetworkUtils.getBaseUrl(url)?.let {
            baseUrl = it
        }
        if (urlArray.size > 1) {
            val options = GSON.fromJsonObject<Map<String, String>>(urlArray[1])
            options?.let {
                options["method"]?.let { if (it.equals("POST", true)) method = RequestMethod.POST }
                options["headers"]?.let { headers ->
                    GSON.fromJsonObject<Map<String, String>>(headers)?.let { headerMap.putAll(it) }
                }
                options["body"]?.let { bodyTxt = it }
                options["charset"]?.let { charset = it }
                options["js"]?.let { webViewJs = it }
                options["sourceRegex"]?.let { sourceRegex = it }
            }
        }
        when (method) {
            RequestMethod.GET -> {
                urlArray = url.split("?")
                url = urlArray[0]
                if (urlArray.size > 1) {
                    analyzeFields(urlArray[1])
                }
            }
            RequestMethod.POST -> {
                bodyTxt?.let {
                    if (it.isJson()) {
                        body = it.toRequestBody(jsonType)
                    } else {
                        analyzeFields(it)
                    }
                } ?: let {
                    body = FormBody.Builder().build()
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
        book: BaseBook?
    ): Any {
        val bindings = SimpleBindings()
        bindings["java"] = JsExtensions
        bindings["page"] = page
        bindings["key"] = key
        bindings["book"] = book
        bindings["result"] = result
        bindings["baseUrl"] = baseUrl
        return SCRIPT_ENGINE.eval(jsStr, bindings)
    }

    @Throws(Exception::class)
    fun getResponse(): Call<String> {
        return when {
            method == RequestMethod.POST -> {
                if (fieldMap.isNotEmpty()) {
                    HttpHelper
                        .getApiService<IHttpPostApi>(baseUrl)
                        .postMap(url, fieldMap, headerMap)
                } else {
                    HttpHelper
                        .getApiService<IHttpPostApi>(baseUrl)
                        .postBody(url, body!!, headerMap)
                }
            }
            fieldMap.isEmpty() -> HttpHelper
                .getApiService<IHttpGetApi>(baseUrl)
                .get(url, headerMap)
            else -> HttpHelper
                .getApiService<IHttpGetApi>(baseUrl)
                .getMap(url, fieldMap, headerMap)
        }
    }

    @Throws(Exception::class)
    fun getResponseAsync(): Deferred<Response<String>> {
        return when {
            method == RequestMethod.POST -> {
                if (fieldMap.isNotEmpty()) {
                    HttpHelper
                        .getApiService<IHttpPostApi>(baseUrl)
                        .postMapAsync(url, fieldMap, headerMap)
                } else {
                    HttpHelper
                        .getApiService<IHttpPostApi>(baseUrl)
                        .postBodyAsync(url, body!!, headerMap)
                }
            }
            fieldMap.isEmpty() -> HttpHelper
                .getApiService<IHttpGetApi>(baseUrl)
                .getAsync(url, headerMap)
            else -> HttpHelper
                .getApiService<IHttpGetApi>(baseUrl)
                .getMapAsync(url, fieldMap, headerMap)
        }
    }

    suspend fun getResultByWebView(tag: String): String {
        val params = AjaxWebView.AjaxParams(tag)
        params.url = url
        params.requestMethod = method
        params.javaScript = webViewJs
        params.sourceRegex = sourceRegex
        params.postData = bodyTxt?.toByteArray()
        return HttpHelper.ajax(params)
    }

}
