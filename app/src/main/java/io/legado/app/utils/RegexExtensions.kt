package io.legado.app.utils

import com.script.SimpleBindings
import io.legado.app.constant.AppConst
import io.legado.app.exception.RegexTimeoutException

/**
 * 带有超时检测的正则替换
 */
fun CharSequence.replace(regex: Regex, replacement: String, timeout: Long): String {
    val startTime = System.currentTimeMillis()
    val charSequence = this
    val isJs = replacement.startsWith("@js:")
    val replacement1 = if (isJs) replacement.substring(4) else replacement
    val pattern = regex.toPattern()
    val matcher = pattern.matcher(charSequence)
    val stringBuffer = StringBuffer()
    while (matcher.find()) {
        if (System.currentTimeMillis() - startTime > timeout) {
            val timeoutMsg = "替换超时,将禁用替换规则"
            throw RegexTimeoutException(timeoutMsg)
        }
        if (isJs) {
            val bindings = SimpleBindings()
            bindings["result"] = matcher.group()
            val jsResult = AppConst.SCRIPT_ENGINE.eval(replacement1, bindings).toString()
            matcher.appendReplacement(stringBuffer, jsResult)
        } else {
            matcher.appendReplacement(stringBuffer, replacement1)
        }
    }
    matcher.appendTail(stringBuffer)
    return stringBuffer.toString()
}

