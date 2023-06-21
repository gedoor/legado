package io.legado.app.help.source

import io.legado.app.constant.AppLog
import io.legado.app.data.entities.BaseSource
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.CacheManager
import io.legado.app.help.IntentData
import io.legado.app.ui.association.VerificationCodeActivity
import io.legado.app.ui.browser.WebViewActivity
import io.legado.app.utils.startActivity
import splitties.init.appCtx
import java.util.concurrent.locks.LockSupport
import kotlin.time.Duration.Companion.minutes

/**
 * 源验证
 */
object SourceVerificationHelp {

    private val waitTime = 1.minutes.inWholeNanoseconds

    private fun getKey(source: BaseSource) = getKey(source.getKey())
    fun getKey(sourceKey: String) = "${sourceKey}_verificationResult"

    /**
     * 获取书源验证结果
     * 图片验证码 防爬 滑动验证码 点击字符 等等
     */
    fun getVerificationResult(
        source: BaseSource?,
        url: String,
        title: String,
        useBrowser: Boolean
    ): String {
        source
            ?: throw NoStackTraceException("getVerificationResult parameter source cannot be null")

        val key = getKey(source)
        CacheManager.delete(key)

        if (!useBrowser) {
            appCtx.startActivity<VerificationCodeActivity> {
                putExtra("imageUrl", url)
                putExtra("sourceOrigin", source.getKey())
                putExtra("sourceName", source.getTag())
                IntentData.put(key, Thread.currentThread())
            }
        } else {
            startBrowser(source, url, title, true)
        }

        var waitUserInput = false
        while (CacheManager.get(key) == null) {
            if (!waitUserInput) {
                AppLog.putDebug("等待返回验证结果...")
                waitUserInput = true
            }
            LockSupport.parkNanos(this, waitTime)
        }

        return CacheManager.get(key)!!.let {
            it.ifBlank {
                throw NoStackTraceException("验证结果为空")
            }
        }
    }

    /**
     * 启动内置浏览器
     * @param saveResult 保存网页源代码到数据库
     */
    fun startBrowser(
        source: BaseSource?,
        url: String,
        title: String,
        saveResult: Boolean? = false
    ) {
        source ?: throw NoStackTraceException("startBrowser parameter source cannot be null")
        val key = getKey(source)
        appCtx.startActivity<WebViewActivity> {
            putExtra("title", title)
            putExtra("url", url)
            putExtra("sourceOrigin", source.getKey())
            putExtra("sourceName", source.getTag())
            putExtra("sourceVerificationEnable", saveResult)
            IntentData.put(url, source.getHeaderMap(true))
            IntentData.put(key, Thread.currentThread())
        }
    }


    fun checkResult(key: String) {
        CacheManager.get(key) ?: CacheManager.putMemory(key, "")
        val thread = IntentData.get<Thread>(key)
        LockSupport.unpark(thread)
    }
}
