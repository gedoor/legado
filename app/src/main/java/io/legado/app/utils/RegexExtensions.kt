package io.legado.app.utils

import com.google.re2j.Pattern
import com.script.SimpleBindings
import io.legado.app.constant.AppConst

/**
 * 带有js功能的正则替换
 * 采用com.google.re2j.Pattern,不会导致无限执行
 */
fun CharSequence.replaceRegex(regex: String, replacement: String): Result<String> {
    return kotlin.runCatching {
        val charSequence = this
        val isJs = replacement.startsWith("@js:")
        val replacement1 = if (isJs) replacement.substring(4) else replacement
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(charSequence)
        val stringBuffer = StringBuffer()
        while (matcher.find()) {
            if (isJs) {
                val bindings = SimpleBindings()
                bindings["result"] = matcher.group()
                val jsResult =
                    AppConst.SCRIPT_ENGINE.eval(replacement1, bindings).toString()
                matcher.appendReplacement(stringBuffer, jsResult)
            } else {
                matcher.appendReplacement(stringBuffer, replacement1)
            }
        }
        matcher.appendTail(stringBuffer)
        stringBuffer.toString()
    }
}

