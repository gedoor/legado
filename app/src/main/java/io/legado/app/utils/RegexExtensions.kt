package io.legado.app.utils

import io.legado.app.exception.NoStackTraceException
import java.util.regex.Pattern

fun CharSequence.regexReplace(regex: String, replacement: String, timeout: Long): String {
    val timeEnd = System.currentTimeMillis() + timeout
    val pattern = Pattern.compile(regex)
    val matcher = pattern.matcher(this)
    var result: Boolean = matcher.find()
    if (result) {
        val sb = StringBuffer()
        do {
            //matcher.appendReplacement(sb, replacement)
            if (System.currentTimeMillis() > timeEnd) {
                throw NoStackTraceException("替换超时")
            }
            result = matcher.find()
        } while (result)
        matcher.appendTail(sb)
        return sb.toString()
    }
    return this.toString()
}
