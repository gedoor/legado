package io.legado.app.utils
// import org.apache.commons.text.StringEscapeUtils

fun String?.safeTrim() = if (this.isNullOrBlank()) null else this.trim()

fun String.isAbsUrl() = this.startsWith("http://", true)
        || this.startsWith("https://", true)

fun String.splitNotBlank(delim: String) = if (!this.contains(delim)) sequenceOf(this) else
        this.split(delim).asSequence().map { it.trim() }.filterNot { it.isBlank() }

// fun String.unescapeJson() = StringEscapeUtils.unescapeJson(this)