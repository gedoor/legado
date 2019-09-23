package io.legado.app.model.analyzeRule

import android.annotation.SuppressLint
import android.text.TextUtils
import androidx.annotation.Keep
import io.legado.app.constant.AppConst.SCRIPT_ENGINE
import io.legado.app.constant.Pattern.EXP_PATTERN
import io.legado.app.data.api.IHttpGetApi
import io.legado.app.data.api.IHttpPostApi
import io.legado.app.data.entities.Book
import io.legado.app.help.http.HttpHelper
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
    book: Book? = null
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
    private lateinit var body: RequestBody
    private var method = Method.GET

    val postData: ByteArray
        get() {
            val builder = StringBuilder()
            val keys = fieldMap.keys
            for (key in keys) {
                builder.append(String.format("%s=%s&", key, fieldMap[key]))
            }
            builder.deleteCharAt(builder.lastIndexOf("&"))
            return builder.toString().toByteArray()
        }

    init {
        baseUrl?.let {
            this.baseUrl = it.split(",[^\\{]*".toRegex(), 1)[0]
        }
        headerMapF?.let { headerMap.putAll(it) }
        //替换参数
        replaceKeyPageJs(key, page, book)
        //处理URL
        initUrl()
    }

    /**
     * 替换关键字,页数,JS
     */
    private fun replaceKeyPageJs(key: String?, page: Int?, book: Book?) {
        //page
        page?.let {
            val matcher = pagePattern.matcher(ruleUrl)
            while (matcher.find()) {
                val pages =
                    matcher.group(1).splitNotBlank(",")
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
                options["method"]?.let { if (it.equals("POST", true)) method = Method.POST }
                options["headers"]?.let { headers ->
                    GSON.fromJsonObject<Map<String, String>>(headers)?.let { headerMap.putAll(it) }
                }
                options["body"]?.let { bodyTxt = it }
                options["charset"]?.let { charset = it }
            }
        }
        when (method) {
            Method.GET -> {
                urlArray = url.split("?")
                url = urlArray[0]
                if (urlArray.size > 1) {
                    analyzeFields(urlArray[1])
                }
            }
            Method.POST -> {
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
                fieldMap[queryM[0]] = Encoder.escape(value)
            } else {
                fieldMap[queryM[0]] = URLEncoder.encode(value, charset)
            }
        }
    }

    enum class Method {
        GET, POST
    }

    fun getResponse(): Call<String> {
        return when {
            method == Method.POST -> {
                if (fieldMap.isNotEmpty()) {
                    HttpHelper.getApiService<IHttpPostApi>(
                        baseUrl
                    ).postMap(url, fieldMap, headerMap)
                } else {
                    HttpHelper.getApiService<IHttpPostApi>(
                        baseUrl
                    ).postBody(
                        url,
                        body,
                        headerMap
                    )
                }
            }
            fieldMap.isEmpty() -> HttpHelper.getApiService<IHttpGetApi>(
                baseUrl
            ).get(url, headerMap)
            else -> HttpHelper.getApiService<IHttpGetApi>(baseUrl)
                .getMap(url, fieldMap, headerMap)
        }
    }

    fun getResponseAsync(): Deferred<Response<String>> {
        return when {
            method == Method.POST -> {
                if (fieldMap.isNotEmpty()) {
                    HttpHelper.getApiService<IHttpPostApi>(
                        baseUrl
                    ).postMapAsync(url, fieldMap, headerMap)
                } else {
                    HttpHelper.getApiService<IHttpPostApi>(
                        baseUrl
                    ).postBodyAsync(
                        url,
                        body,
                        headerMap
                    )
                }
            }
            fieldMap.isEmpty() -> HttpHelper.getApiService<IHttpGetApi>(
                baseUrl
            ).getAsync(url, headerMap)
            else -> HttpHelper.getApiService<IHttpGetApi>(baseUrl)
                .getMapAsync(url, fieldMap, headerMap)
        }
    }
}
