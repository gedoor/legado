package io.legado.app.utils

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