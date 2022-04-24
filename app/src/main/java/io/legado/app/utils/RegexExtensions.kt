package io.legado.app.utils

import io.legado.app.exception.RegexTimeoutException
import io.legado.app.help.CrashHandler
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.init.appCtx
import kotlin.coroutines.resume

private val scope = MainScope()

/**
 * 带有超时检测的正则替换
 * 超时重启apk,线程不能强制结束,只能重启apk
 */
suspend fun CharSequence.replace(regex: Regex, replacement: String, timeout: Long): String {
    val charSequence = this
    return suspendCancellableCoroutine { block ->
        val job = scope.launch(IO) {
            try {
                val result = regex.replace(charSequence, replacement)
                block.resume(result)
            } catch (e: Exception) {
                block.cancel(e)
            }
        }
        mainHandler.postDelayed({
            if (job.isActive) {
                val timeoutMsg = "替换超时,将在3秒后重启应用\n替换规则$regex\n替换内容:${this}"
                val exception = RegexTimeoutException(timeoutMsg)
                block.cancel(exception)
                appCtx.longToastOnUi(timeoutMsg)
                CrashHandler.saveCrashInfo2File(exception)
                mainHandler.postDelayed({
                    appCtx.restart()
                }, 3000)
            }
        }, timeout)
    }
}

