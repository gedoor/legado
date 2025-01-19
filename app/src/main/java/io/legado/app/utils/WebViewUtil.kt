package io.legado.app.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.annotation.RequiresApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object WebViewUtil {
    const val SPOOF_PACKAGE_NAME = "org.chromium.chrome"

    const val MINIMUM_WEBVIEW_VERSION = 118

    /**
     * 使用 WebView 的用户代理字符串创建类似于 Android 上的 Chrome 的内容
     * 会返回。
     *
     * WebView 用户代理字符串示例：
     * Mozilla/5.0（Linux；Android 13；Pixel 7 Build/TQ3A.230901.001；wv）AppleWebKit/537.36（KHTML，如 Gecko）版本/4.0 Chrome/116.0.0.0 Mobile Safari/537.36
     *
     * Android 上的 Chrome 示例：
     * Mozilla/5.0（Linux；Android 10；K）AppleWebKit/537.36（KHTML，如 Gecko）Chrome/116.0.0.0 Mobile Safari/537.3
     */
    fun getInferredUserAgent(context: Context): String {
        return WebView(context)
            .getDefaultUserAgentString()
            .replace("; Android .*?\\)".toRegex(), "; Android 10; K)")
            .replace("Version/.* Chrome/".toRegex(), "Chrome/")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getVersion(context: Context): String {
        val webView = WebView.getCurrentWebViewPackage() ?: return "未找到WebView？"
        val pm = context.packageManager
        val label = webView.applicationInfo!!.loadLabel(pm)
        val version = webView.versionName
        return "$label $version"
    }

    fun supportsWebView(context: Context): Boolean {
        try {
            // 如果 WebView 可能会抛出 android.webkit.WebViewFactory$MissingWebViewPackageException
            // 未安装
            CookieManager.getInstance()
        } catch (e: Throwable) {
            e.printOnDebug()
            return false
        }

        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW)
    }
}

fun WebView.isOutdated(): Boolean {
    return getWebViewMajorVersion() < WebViewUtil.MINIMUM_WEBVIEW_VERSION
}

suspend fun WebView.getHtml(): String = suspendCancellableCoroutine {
    evaluateJavascript("document.documentElement.outerHTML") { html -> it.resume(html) }
}

@SuppressLint("SetJavaScriptEnabled")
fun WebView.setDefaultSettings() {
    with(settings) {
        javaScriptEnabled = true
        domStorageEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true
        cacheMode = WebSettings.LOAD_DEFAULT

        // Allow zooming
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
    }

    CookieManager.getInstance().acceptThirdPartyCookies(this)
}

private fun WebView.getWebViewMajorVersion(): Int {
    val uaRegexMatch = """.*Chrome/(\d+)\..*""".toRegex().matchEntire(getDefaultUserAgentString())
    return if (uaRegexMatch != null && uaRegexMatch.groupValues.size > 1) {
        uaRegexMatch.groupValues[1].toInt()
    } else {
        0
    }
}

// 基于 https://stackoverflow.com/a/29218966
private fun WebView.getDefaultUserAgentString(): String {
    val originalUA: String = settings.userAgentString

    // 接下来调用 getUserAgentString() 将获得默认值
    settings.userAgentString = null
    val defaultUserAgentString = settings.userAgentString

    // 恢复为原始 UA 字符串
    settings.userAgentString = originalUA

    return defaultUserAgentString
}
