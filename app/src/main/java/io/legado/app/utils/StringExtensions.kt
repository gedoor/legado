@file:Suppress("unused")

package io.legado.app.utils

import android.icu.text.Collator
import android.icu.util.ULocale
import android.net.Uri
import java.io.File
import java.util.*

val removeHtmlRegex = "</?(?:div|p|br|hr|h\\d|article|dd|dl)[^>]*>".toRegex()
val imgRegex = "<img[^>]*>".toRegex()
val notImgHtmlRegex = "</?(?!img)\\w+[^>]*>".toRegex()

fun String?.safeTrim() = if (this.isNullOrBlank()) null else this.trim()

fun String?.isContentScheme(): Boolean = this?.startsWith("content://") == true

fun String.parseToUri(): Uri {
    return if (isContentScheme()) {
        Uri.parse(this)
    } else {
        Uri.fromFile(File(this))
    }
}

fun String?.isAbsUrl() =
    this?.let {
        it.startsWith("http://", true) || it.startsWith("https://", true)
    } ?: false

fun String?.isJson(): Boolean =
    this?.run {
        val str = this.trim()
        when {
            str.startsWith("{") && str.endsWith("}") -> true
            str.startsWith("[") && str.endsWith("]") -> true
            else -> false
        }
    } ?: false

fun String?.isJsonObject(): Boolean =
    this?.run {
        val str = this.trim()
        str.startsWith("{") && str.endsWith("}")
    } ?: false

fun String?.isJsonArray(): Boolean =
    this?.run {
        val str = this.trim()
        str.startsWith("[") && str.endsWith("]")
    } ?: false

fun String?.htmlFormat(): String {
    this ?: return ""
    return this
        .replace(imgRegex, "\n$0\n")
        .replace(removeHtmlRegex, "\n")
        .replace(notImgHtmlRegex, "")
        .replace("\\s*\\n+\\s*".toRegex(), "\n　　")
        .replace("^[\\n\\s]+".toRegex(), "　　")
        .replace("[\\n\\s]+$".toRegex(), "")
}

fun String.splitNotBlank(vararg delimiter: String): Array<String> = run {
    this.split(*delimiter).map { it.trim() }.filterNot { it.isBlank() }.toTypedArray()
}

fun String.splitNotBlank(regex: Regex, limit: Int = 0): Array<String> = run {
    this.split(regex, limit).map { it.trim() }.filterNot { it.isBlank() }.toTypedArray()
}

fun String.cnCompare(other: String): Int {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        Collator.getInstance(ULocale.SIMPLIFIED_CHINESE).compare(this, other)
    } else {
        java.text.Collator.getInstance(Locale.CHINA).compare(this, other)
    }
}

/**
 * 将字符串拆分为单个字符,包含emoji
 */
fun String.toStringArray(): Array<String> {
    var codePointIndex = 0
    return try {
        Array(codePointCount(0, length)) {
            val start = codePointIndex
            codePointIndex = offsetByCodePoints(start, 1)
            substring(start, codePointIndex)
        }
    } catch (e: Exception) {
        split("").toTypedArray()
    }
}

