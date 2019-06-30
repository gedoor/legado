package io.legado.app.model.analyzeRule

import android.annotation.SuppressLint
import android.text.TextUtils
import androidx.annotation.Keep
import io.legado.app.constant.AppConst.SCRIPT_ENGINE
import io.legado.app.constant.Pattern.EXP_PATTERN
import io.legado.app.data.entities.Book
import io.legado.app.utils.Encoder
import io.legado.app.utils.GSON
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.fromJsonObject
import okhttp3.RequestBody
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
    private var ruleUrl: String,
    key: String? = null,
    page: Int? = null,
    headerMapF: Map<String, String>? = null,
    var baseUrl: String? = null,
    book: Book? = null
) {
    companion object {
        private val pagePattern = Pattern.compile("<(.*?)>")
    }

    lateinit var url: String
        private set
    var host: String? = null
        private set
    var path: String? = null
        private set
    var queryStr: String? = null
        private set
    private val queryMap = LinkedHashMap<String, String>()
    private val headerMap = HashMap<String, String>()
    private var charset: String? = null
    var body: RequestBody? = null
        private set
    var method = Method.GET
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

    init {
        baseUrl = baseUrl?.split(",\n*".toRegex(), 1)?.get(0)
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
                    matcher.group(1).split(",".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                ruleUrl = if (page <= pages.size) {
                    ruleUrl.replace(matcher.group(), pages[page - 1].trim { it <= ' ' })
                } else {
                    ruleUrl.replace(matcher.group(), pages[pages.size - 1].trim { it <= ' ' })
                }
            }
        }
        //js
        if (ruleUrl.contains("{{") && ruleUrl.contains("}}")) {
            var jsEval: Any
            val sb = StringBuffer(ruleUrl.length)
            val simpleBindings = object : SimpleBindings() {
                init {
                    this["baseUrl"] = baseUrl
                    this["page"] = page
                    this["key"] = key
                    this["book"] = book
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
    }

    /**
     * 处理URL
     */
    private fun initUrl() {
        val urlArray = ruleUrl.split(",[^\\{]*".toRegex(), 2)
        url = urlArray[0]
        host = NetworkUtils.getBaseUrl(url)
        if (urlArray.size > 1) {
            val options = GSON.fromJsonObject<Map<String, String>>(urlArray[1])
            options?.let {
                options["method"]?.let { if (it.equals("POST", true)) method = Method.POST }
                options["headers"]?.let { headers ->
                    GSON.fromJsonObject<Map<String, String>>(
                        headers
                    )?.let { headerMap.putAll(it) }
                }
                options["body"]?.let { }
                options["charset"]?.let { charset = it }
            }
        }
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
            if (TextUtils.isEmpty(charset)) {
                if (NetworkUtils.hasUrlEncoded(value)) {
                    queryMap[queryM[0]] = value
                } else {
                    queryMap[queryM[0]] = URLEncoder.encode(value, "UTF-8")
                }
            } else if (charset == "escape") {
                queryMap[queryM[0]] = Encoder.escape(value)
            } else {
                queryMap[queryM[0]] = URLEncoder.encode(value, charset)
            }
        }
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

    enum class Method {
        GET, POST
    }
}
