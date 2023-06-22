package io.legado.app.utils

import androidx.core.os.postDelayed
import com.script.SimpleBindings
import com.script.rhino.RhinoScriptEngine
import io.legado.app.exception.RegexTimeoutException
import io.legado.app.help.CrashHandler
import io.legado.app.help.coroutine.Coroutine
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.init.appCtx
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val handler by lazy { buildMainHandler() }

/**
 * 带有超时检测的正则替换
 */
fun CharSequence.replace(regex: Regex, replacement: String, timeout: Long): String {
    val charSequence = this@replace
    val isJs = replacement.startsWith("@js:")
    val replacement1 = if (isJs) replacement.substring(4) else replacement
    return runBlocking {
        suspendCancellableCoroutine { block ->
            val coroutine = Coroutine.async(executeContext = IO) {
                try {
                    val pattern = regex.toPattern()
                    val matcher = pattern.matcher(charSequence)
                    val stringBuffer = StringBuffer()
                    while (matcher.find()) {
                        if (isJs) {
                            val jsResult = RhinoScriptEngine.run {
                                val bindings = SimpleBindings()
                                bindings["result"] = matcher.group()
                                eval(replacement1, bindings)
                            }.toString()
                            matcher.appendReplacement(stringBuffer, jsResult)
                        } else {
                            matcher.appendReplacement(stringBuffer, replacement1)
                        }
                    }
                    matcher.appendTail(stringBuffer)
                    block.resume(stringBuffer.toString())
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
}

