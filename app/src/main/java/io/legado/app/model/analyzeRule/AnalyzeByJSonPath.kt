package io.legado.app.model.analyzeRule

import android.text.TextUtils
import androidx.annotation.Keep
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.ReadContext
import io.legado.app.utils.splitNotBlank
import java.util.*
import java.util.regex.Pattern

@Suppress("RegExpRedundantEscape")
@Keep
class AnalyzeByJSonPath(json: Any) {

    companion object {
        private val jsonRulePattern = Pattern.compile("(?<=\\{)\\$\\..+?(?=\\})")

        fun parse(json: Any): ReadContext {
            return when (json) {
                is ReadContext -> json
                is String -> JsonPath.parse(json)
                else -> JsonPath.parse(json)
            }
        }
    }

    private var ctx: ReadContext = parse(json)

    fun getString(rule: String): String? {
        if (TextUtils.isEmpty(rule)) return null
        var result = ""
        val rules: Array<String>
        val elementsType: String
        if (rule.contains("&&")) {
            rules = rule.splitNotBlank("&&")
            elementsType = "&"
        } else {
            rules = rule.splitNotBlank("||")
            elementsType = "|"
        }
        if (rules.size == 1) {
            if (!rule.contains("{$.")) {
                try {
                    val ob = ctx.read<Any>(rule)
                    result =
                        if (ob is List<*>) {
                            val builder = StringBuilder()
                            for (o in ob) {
                                builder.append(o).append("\n")
                            }
                            builder.toString().replace("\\n$".toRegex(), "")
                        } else {
                            ob.toString()
                        }
                } catch (ignored: Exception) {
                }
                return result
            } else {
                result = rule
                val matcher = jsonRulePattern.matcher(rule)
                while (matcher.find()) {
                    result = result.replace(
                        String.format("{%s}", matcher.group()),
                        getString(matcher.group())!!
                    )
                }
                return result
            }
        } else {
            val textList = arrayListOf<String>()
            for (rl in rules) {
                val temp = getString(rl)
                if (!temp.isNullOrEmpty()) {
                    textList.add(temp)
                    if (elementsType == "|") {
                        break
                    }
                }
            }
            return textList.joinToString("\n")
        }
    }

    internal fun getStringList(rule: String): List<String> {
        val result = ArrayList<String>()
        if (TextUtils.isEmpty(rule)) return result
        val rules: Array<String>
        val elementsType: String
        when {
            rule.contains("&&") -> {
                rules = rule.splitNotBlank("&&")
                elementsType = "&"
            }
            rule.contains("%%") -> {
                rules = rule.splitNotBlank("%%")
                elementsType = "%"
            }
            else -> {
                rules = rule.splitNotBlank("||")
                elementsType = "|"
            }
        }
        if (rules.size == 1) {
            if (!rule.contains("{$.")) {
                try {
                    val obj = ctx.read<Any>(rule) ?: return result
                    if (obj is List<*>) {
                        for (o in obj)
                            result.add(o.toString())
                    } else {
                        result.add(obj.toString())
                    }
                } catch (ignored: Exception) {
                }
                return result
            } else {
                val matcher = jsonRulePattern.matcher(rule)
                while (matcher.find()) {
                    val stringList = getStringList(matcher.group())
                    for (s in stringList) {
                        result.add(rule.replace(String.format("{%s}", matcher.group()), s))
                    }
                }
                return result
            }
        } else {
            val results = ArrayList<List<String>>()
            for (rl in rules) {
                val temp = getStringList(rl)
                if (temp.isNotEmpty()) {
                    results.add(temp)
                    if (temp.isNotEmpty() && elementsType == "|") {
                        break
                    }
                }
            }
            if (results.size > 0) {
                if ("%" == elementsType) {
                    for (i in results[0].indices) {
                        for (temp in results) {
                            if (i < temp.size) {
                                result.add(temp[i])
                            }
                        }
                    }
                } else {
                    for (temp in results) {
                        result.addAll(temp)
                    }
                }
            }
            return result
        }
    }

    internal fun getObject(rule: String): Any {
        return ctx.read(rule)
    }

    internal fun getList(rule: String): ArrayList<Any>? {
        val result = ArrayList<Any>()
        if (TextUtils.isEmpty(rule)) return result
        val elementsType: String
        val rules: Array<String>
        when {
            rule.contains("&&") -> {
                rules = rule.splitNotBlank("&&")
                elementsType = "&"
            }
            rule.contains("%%") -> {
                rules = rule.splitNotBlank("%%")
                elementsType = "%"
            }
            else -> {
                rules = rule.splitNotBlank("||")
                elementsType = "|"
            }
        }
        if (rules.size == 1) {
            ctx.let {
                try {
                    return it.read<ArrayList<Any>>(rules[0])
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return null
        } else {
            val results = ArrayList<ArrayList<*>>()
            for (rl in rules) {
                val temp = getList(rl)
                if (temp != null && temp.isNotEmpty()) {
                    results.add(temp)
                    if (temp.isNotEmpty() && elementsType == "|") {
                        break
                    }
                }
            }
            if (results.size > 0) {
                if ("%" == elementsType) {
                    for (i in 0 until results[0].size) {
                        for (temp in results) {
                            if (i < temp.size) {
                                temp[i]?.let { result.add(it) }
                            }
                        }
                    }
                } else {
                    for (temp in results) {
                        result.addAll(temp)
                    }
                }
            }
        }
        return result
    }

}
