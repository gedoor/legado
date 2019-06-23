package io.legado.app.utils

// import org.apache.commons.text.StringEscapeUtils

fun String?.safeTrim() = if (this.isNullOrBlank()) null else this.trim()

fun String.isAbsUrl() = this.startsWith("http://", true)
        || this.startsWith("https://", true)

fun String.splitNotBlank(delim: String) = if (!this.contains(delim)) sequenceOf(this) else
    this.split(delim).asSequence().map { it.trim() }.filterNot { it.isBlank() }

fun startWithIgnoreCase(src: String?, obj: String?): Boolean {
    if (src == null || obj == null) return false
    return if (obj.length > src.length) false else src.substring(0, obj.length).equals(obj, ignoreCase = true)
}