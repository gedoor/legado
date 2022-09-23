package io.legado.app.utils

import androidx.core.os.postDelayed
import io.legado.app.exception.RegexTimeoutException
import io.legado.app.help.CrashHandler
import io.legado.app.help.coroutine.Coroutine
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.init.appCtx
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 带有超时检测的正则替换
 * 超时重启apk,线程不能强制结束,只能重启apk
 */
suspend fun CharSequence.replace(regex: Regex, replacement: String, timeout: Long): String {
    val charSequence = this
    return suspendCancellableCoroutine { block ->
        val coroutine = Coroutine.async {
            try {
                val result = regex.replace(charSequence, replacement)
                block.resume(result)
            } catch (e: Exception) {
                block.resumeWithException(e)
            }
        }
        mainHandler.postDelayed(timeout) {
            if (coroutine.isActive) {
                val timeoutMsg = "替换超时,3秒后还未结束将重启应用\n替换规则$regex\n替换内容:${this}"
                val exception = RegexTimeoutException(timeoutMsg)
                block.cancel(exception)
                appCtx.longToastOnUi(timeoutMsg)
                CrashHandler.saveCrashInfo2File(exception)
                mainHandler.postDelayed(3000) {
                    if (coroutine.isActive) {
                        appCtx.restart()
                    }
                }
            }
        }
    }
}

