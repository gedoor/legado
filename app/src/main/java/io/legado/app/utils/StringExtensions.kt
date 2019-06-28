package io.legado.app.utils

import android.text.TextUtils

// import org.apache.commons.text.StringEscapeUtils

fun String?.safeTrim() = if (this.isNullOrBlank()) null else this.trim()

fun String.isAbsUrl() = this.startsWith("http://", true)
        || this.startsWith("https://", true)

fun String.isJson(): Boolean = kotlin.run {
    var result = false
    if (!TextUtils.isEmpty(this)) {
        val str = this.trim()
        if (str.startsWith("{") && str.endsWith("}")) {
            result = true
        } else if (str.startsWith("[") && str.endsWith("]")) {
            result = true
        }
    }
    return result
}

fun String.htmlFormat(): String = if (TextUtils.isEmpty(this)) "" else
    this.replace("(?i)<(br[\\s/]*|/*p.*?|/*div.*?)>".toRegex(), "\n")// 替换特定标签为换行符
        .replace("<[script>]*.*?>|&nbsp;".toRegex(), "")// 删除script标签对和空格转义符
        .replace("\\s*\\n+\\s*".toRegex(), "\n　　")// 移除空行,并增加段前缩进2个汉字
        .replace("^[\\n\\s]+".toRegex(), "　　")//移除开头空行,并增加段前缩进2个汉字
        .replace("[\\n\\s]+$".toRegex(), "") //移除尾部空行

fun String.splitNotBlank(delim: String) = if (!this.contains(delim)) sequenceOf(this) else
    this.split(delim).asSequence().map { it.trim() }.filterNot { it.isBlank() }

fun startWithIgnoreCase(src: String?, obj: String?): Boolean {
    if (src == null || obj == null) return false
    return if (obj.length > src.length) false else src.substring(0, obj.length).equals(obj, ignoreCase = true)
}