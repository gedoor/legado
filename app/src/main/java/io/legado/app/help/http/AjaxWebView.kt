package io.legado.app.help.http

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import io.legado.app.App
import io.legado.app.constant.AppConst
import org.apache.commons.text.StringEscapeUtils
import java.lang.ref.WeakReference


class AjaxWebView {
    var callback: Callback? = null
    private var mHandler: AjaxHandler

    init {
        mHandler = AjaxHandler(this)
    }

    class AjaxHandler(private val ajaxWebView: AjaxWebView) : Handler(Looper.getMainLooper()) {

        private var mWebView: WebView? = null

        override fun handleMessage(msg: Message) {
            val params: AjaxParams
            when (msg.what) {
                MSG_AJAX_START -> {
                    params = msg.obj as AjaxParams
                    mWebView = createAjaxWebView(params, this)
                }
                MSG_SNIFF_START -> {
                    params = msg.obj as AjaxParams
                    mWebView = createAjaxWebView(params, this)
                }
                MSG_SUCCESS -> {
                    ajaxWebView.callback?.onResult(msg.obj as StrResponse)
                    destroyWebView()
                }
                MSG_ERROR -> {
                    ajaxWebView.callback?.onError(msg.obj as Throwable)
                    destroyWebView()
                }
            }
        }

        @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
        fun createAjaxWebView(params: AjaxParams, handler: Handler): WebView {
            val webView = WebView(App.INSTANCE)
            val settings = webView.settings
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.blockNetworkImage = true
            settings.userAgentString = params.userAgent
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            if (params.isSniff) {
                webView.webViewClient = SnifferWebClient(params, handler)
            } else {
                webView.webViewClient = HtmlWebViewClient(params, handler)
            }
            when (params.requestMethod) {
                RequestMethod.POST -> params.postData?.let {
                    webView.postUrl(params.url, it)
                }
                RequestMethod.GET -> params.headerMap?.let {
                    webView.loadUrl(params.url, it)
                }
            }
            return webView
        }

        private fun destroyWebView() {
            mWebView?.destroy()
            mWebView = null
        }
    }

    fun load(params: AjaxParams) {
        if (params.sourceRegex != "") {
            mHandler.obtainMessage(MSG_SNIFF_START, params)
                .sendToTarget()
        } else {
            mHandler.obtainMessage(MSG_AJAX_START, params)
                .sendToTarget()
        }
    }

    fun destroyWebView() {
        mHandler.obtainMessage(DESTROY_WEB_VIEW)
    }

    class AjaxParams(val url: String) {
        var tag: String? = null
        var requestMethod = RequestMethod.GET
        var postData: ByteArray? = null
        var headerMap: Map<String, String>? = null
        var sourceRegex: String? = null
        var javaScript: String? = null

        fun getJs(): String {
            javaScript?.let {
                if (it.isNotEmpty()) {
                    return it
                }
            }
            return JS
        }

        val userAgent: String?
            get() = this.headerMap?.get(AppConst.UA_NAME)

        val isSniff: Boolean
            get() = !TextUtils.isEmpty(sourceRegex)

        fun setCookie(url: String) {
            tag?.let {
                val cookie = CookieManager.getInstance().getCookie(url)
                CookieStore.setCookie(it, cookie)
            }
        }

        fun hasJavaScript(): Boolean {
            return !TextUtils.isEmpty(javaScript)
        }

        fun clearJavaScript() {
            javaScript = null
        }

    }

    private class HtmlWebViewClient(
        private val params: AjaxParams,
        private val handler: Handler
    ) : WebViewClient() {

        override fun onPageFinished(view: WebView, url: String) {
            params.setCookie(url)
            val runnable = EvalJsRunnable(view, url, params.getJs(), handler)
            handler.postDelayed(runnable, 1000)
        }

    }

    private class EvalJsRunnable(
        webView: WebView,
        private val url: String,
        private val mJavaScript: String,
        private val handler: Handler
    ) : Runnable {
        var retry = 0
        private val mWebView: WeakReference<WebView> = WeakReference(webView)
        override fun run() {
            mWebView.get()?.evaluateJavascript(mJavaScript) {
                if (it.isNotEmpty() && it != "null") {
                    val content = StringEscapeUtils.unescapeJson(it)
                        .replace("^\"|\"$".toRegex(), "")
                    try {
                        val response = StrResponse(url, content)
                        handler.obtainMessage(MSG_SUCCESS, response).sendToTarget()
                    } catch (e: Exception) {
                        handler.obtainMessage(MSG_ERROR, e).sendToTarget()
                    }
                    handler.removeCallbacks(this)
                    return@evaluateJavascript
                }
                if (retry > 30) {
                    handler.obtainMessage(MSG_ERROR, Exception("js执行超时"))
                        .sendToTarget()
                    handler.removeCallbacks(this)
                    return@evaluateJavascript
                }
                retry++
                handler.removeCallbacks(this)
                handler.postDelayed(this, 1000)
            }
        }
    }

    private class SnifferWebClient(
        private val params: AjaxParams,
        private val handler: Handler
    ) : WebViewClient() {

        override fun onLoadResource(view: WebView, url: String) {
            params.sourceRegex?.let {
                if (url.matches(it.toRegex())) {
                    try {
                        val response = StrResponse(params.url, url)
                        handler.obtainMessage(MSG_SUCCESS, response).sendToTarget()
                    } catch (e: Exception) {
                        handler.obtainMessage(MSG_ERROR, e).sendToTarget()
                    }
                }
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            params.setCookie(url)
            if (params.hasJavaScript()) {
                evaluateJavascript(view, params.javaScript)
                params.clearJavaScript()
            }
        }

        private fun evaluateJavascript(webView: WebView, javaScript: String?) {
            val runnable = LoadJsRunnable(webView, javaScript)
            handler.postDelayed(runnable, 1000L)
        }
    }

    private class LoadJsRunnable(
        webView: WebView,
        private val mJavaScript: String?
    ) : Runnable {
        private val mWebView: WeakReference<WebView> = WeakReference(webView)
        override fun run() {
            mWebView.get()?.loadUrl("javascript:${mJavaScript ?: ""}")
        }
    }

    companion object {
        const val MSG_AJAX_START = 0
        const val MSG_SNIFF_START = 1
        const val MSG_SUCCESS = 2
        const val MSG_ERROR = 3
        const val DESTROY_WEB_VIEW = 4
        const val JS = "document.documentElement.outerHTML"
    }

    abstract class Callback {
        abstract fun onResult(response: StrResponse)
        abstract fun onError(error: Throwable)
    }
}