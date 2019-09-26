package io.legado.app.utils

import kotlin.math.min

// import org.apache.commons.text.StringEscapeUtils

fun String?.safeTrim() = if (this.isNullOrBlank()) null else this.trim()

fun String?.isAbsUrl() = if (this.isNullOrBlank()) false else this.startsWith("http://", true)
        || this.startsWith("https://", true)

fun String?.isJson(): Boolean = this?.run {
    val str = this.trim()
    when {
        str.startsWith("{") && str.endsWith("}") -> true
        str.startsWith("[") && str.endsWith("]") -> true
        else -> false
    }
} ?: false

fun String?.isJsonObject(): Boolean = this?.run {
    val str = this.trim()
    when {
        str.startsWith("{") && str.endsWith("}") -> true
        else -> false
    }
} ?: false

fun String?.isJsonArray(): Boolean = this?.run {
    val str = this.trim()
    when {
        str.startsWith("[") && str.endsWith("]") -> true
        else -> false
    }
} ?: false

fun String?.htmlFormat(): String = if (this.isNullOrBlank()) "" else
    this.replace("(?i)<(br[\\s/]*|/*p\\b.*?|/*div\\b.*?)>".toRegex(), "\n")// 替换特定标签为换行符
        .replace("<[script>]*.*?>|&nbsp;".toRegex(), "")// 删除script标签对和空格转义符
        .replace("\\s*\\n+\\s*".toRegex(), "\n　　")// 移除空行,并增加段前缩进2个汉字
        .replace("^[\\n\\s]+".toRegex(), "　　")//移除开头空行,并增加段前缩进2个汉字
        .replace("[\\n\\s]+$".toRegex(), "") //移除尾部空行

fun String.splitNotBlank(vararg delimiter: String): Array<String> = run {
    this.split(*delimiter).map { it.trim() }.filterNot { it.isBlank() }.toTypedArray()
}

fun String.splitNotBlank(regex: Regex, limit: Int = 0): Array<String> = run {
    this.split(regex, limit).map { it.trim() }.filterNot { it.isBlank() }.toTypedArray()
}

fun String.startWithIgnoreCase(start: String): Boolean {
    return if (this.isBlank()) false else startsWith(start, true)
}

/**
 * 计算相似度
 */
fun String.similarity(target: String): Float {
    //计算两个字符串的长度。
    val len1 = this.length
    val len2 = target.length
    //建立上面说的数组，比字符长度大一个空间
    val dif = Array(len1 + 1) { IntArray(len2 + 1) }
    //赋初值，步骤B。
    for (a in 0..len1) {
        dif[a][0] = a
    }
    for (a in 0..len2) {
        dif[0][a] = a
    }
    //计算两个字符是否一样，计算左上的值
    var temp: Int
    for (i in 1..len1) {
        for (j in 1..len2) {
            temp = if (this[i - 1] == target[j - 1]) {
                0
            } else {
                1
            }
            //取三个值中最小的
            dif[i][j] = min(
                min(dif[i - 1][j - 1] + temp, dif[i][j - 1] + 1),
                dif[i - 1][j] + 1
            )
        }
    }
    //计算相似度
    return 1 - dif[len1][len2].toFloat() / Math.max(length, target.length)
}

