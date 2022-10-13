package io.legado.app.utils

import androidx.core.os.postDelayed
import com.script.SimpleBindings
import io.legado.app.constant.AppConst
import io.legado.app.exception.RegexTimeoutException
import io.legado.app.help.CrashHandler
import io.legado.app.help.coroutine.Coroutine
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.init.appCtx
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
        val coroutine = Coroutine.async {
            try {
                if (replacement.startsWith("@js:")) {
                    val js = replacement.substring(4)
                    val pattern = regex.toPattern()
                    val matcher = pattern.matcher(charSequence)
                    val stringBuffer = StringBuffer()
                    while (matcher.find()) {
                        val bindings = SimpleBindings()
                        bindings["result"] = matcher.group(1)
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
        handler.postDelayed(timeout) {
            if (coroutine.isActive) {
                val timeoutMsg = "替换超时,3秒后还未结束将重启应用\n替换规则$regex\n替换内容:${this}"
                val exception = RegexTimeoutException(timeoutMsg)
                block.cancel(exception)
                appCtx.longToastOnUi(timeoutMsg)
                CrashHandler.saveCrashInfo2File(exception)
                handler.postDelayed(3000) {
                    if (coroutine.isActive) {
                        appCtx.restart()
                    }
                }
            }
        }
    }
}

