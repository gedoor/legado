package io.legado.app.utils

import androidx.core.os.postDelayed
import com.script.SimpleBindings
import io.legado.app.constant.AppConst
import io.legado.app.exception.RegexTimeoutException
import io.legado.app.help.CrashHandler
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val handler by lazy { buildMainHandler() }

/**
 * 带有超时检测的正则替换
 * 超时重启apk,线程不能强制结束,只能重启apk
 */
suspend fun CharSequence.replace(regex: Regex, replacement: String, timeout: Long): String {
    val charSequence = this
    return suspendCancellableCoroutine { block ->
        val thread = Thread {
            try {
                if (replacement.startsWith("@js:")) {
                    val js = replacement.substring(4)
                    val pattern = regex.toPattern()
                    val matcher = pattern.matcher(charSequence)
                    val stringBuffer = StringBuffer()
                    while (matcher.find()) {
                        val bindings = SimpleBindings()
                        bindings["result"] = matcher.group()
                        val jsResult = AppConst.SCRIPT_ENGINE.eval(js, bindings).toString()
                        matcher.appendReplacement(stringBuffer, jsResult)
                    }
                    matcher.appendTail(stringBuffer)
                    block.resume(stringBuffer.toString())
                } else {
                    val result = regex.replace(charSequence, replacement)
                    block.resume(result)
                }
            } catch (e: Exception) {
                block.resumeWithException(e)
            }
        }
        thread.start()
        handler.postDelayed(timeout) {
            if (thread.isAlive) {
                runCatching {
                    @Suppress("DEPRECATION")
                    thread.stop()
                }
                val timeoutMsg = "替换超时,将禁用替换规则"
                val exception = RegexTimeoutException(timeoutMsg)
                block.cancel(exception)
                CrashHandler.saveCrashInfo2File(exception)
            }
        }
    }
}

