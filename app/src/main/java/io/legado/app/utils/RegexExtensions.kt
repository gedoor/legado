package io.legado.app.utils

import io.legado.app.exception.RegexTimeoutException
import io.legado.app.help.CrashHandler
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.init.appCtx
import kotlin.concurrent.thread
import kotlin.coroutines.resume

suspend fun CharSequence.replace(regex: Regex, replacement: String, timeout: Long): String {
    return suspendCancellableCoroutine { block ->
        val thread = thread {
            try {
                val result = regex.replace(this, replacement)
                block.resume(result)
            } catch (e: Exception) {
                block.cancel(e)
            }
        }
        mainHandler.postDelayed({
            if (thread.isAlive) {
                val timeoutMsg = "替换超时,将在3秒后重启应用\n替换规则$regex\n替换内容:${this}"
                val exception = RegexTimeoutException(timeoutMsg)
                block.cancel(exception)
                appCtx.longToastOnUi(timeoutMsg)
                CrashHandler.saveCrashInfo2File(exception)
                mainHandler.postDelayed({
                    if (thread.isAlive) {
                        appCtx.restart()
                    }
                }, 3000)
            }
        }, timeout)
    }
}

