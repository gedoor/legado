package io.legado.app.ui.web

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.*
import androidx.activity.viewModels
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.databinding.ActivityWebViewBinding
import io.legado.app.help.AppConfig
import io.legado.app.lib.dialogs.SelectItem
import io.legado.app.model.Download
import io.legado.app.ui.association.OnLineImportActivity
import io.legado.app.ui.document.HandleFileContract
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding

class WebViewActivity : VMBaseActivity<ActivityWebViewBinding, WebViewModel>() {

    override val binding by viewBinding(ActivityWebViewBinding::inflate)
    override val viewModel by viewModels<WebViewModel>()
    private val imagePathKey = "imagePath"
    private var customWebViewCallback: WebChromeClient.CustomViewCallback? = null
    private var webPic: String? = null
    private val saveImage = registerForActivityResult(HandleFileContract()) {
        it ?: return@registerForActivityResult
        ACache.get(this).put(imagePathKey, it.toString())
        viewModel.saveImage(webPic, it.toString())
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initWebView()

    }

    @SuppressLint("JavascriptInterface")
    private fun initWebView() {
        binding.webView.webChromeClient = CustomWebChromeClient()
        binding.webView.webViewClient = CustomWebViewClient()
        binding.webView.settings.apply {
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            domStorageEnabled = true
            allowContentAccess = true
        }
        binding.webView.addJavascriptInterface(this, "app")
        upWebViewTheme()
        binding.webView.setOnLongClickListener {
            val hitTestResult = binding.webView.hitTestResult
            if (hitTestResult.type == WebView.HitTestResult.IMAGE_TYPE ||
                hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
            ) {
                hitTestResult.extra?.let {
                    saveImage(it)
                    return@setOnLongClickListener true
                }
            }
            return@setOnLongClickListener false
        }
        binding.webView.setDownloadListener { url, _, contentDisposition, _, _ ->
            val fileName = URLUtil.guessFileName(url, contentDisposition, null)
            binding.llView.longSnackbar(fileName, getString(R.string.action_download)) {
                Download.start(this, url, fileName)
            }
        }
    }

    private fun upWebViewTheme() {
        if (AppConfig.isNightTheme) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                WebSettingsCompat.setForceDarkStrategy(
                    binding.webView.settings,
                    WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
                )
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(
                    binding.webView.settings,
                    WebSettingsCompat.FORCE_DARK_ON
                )
            } else {
                binding.webView
                    .evaluateJavascript(AppConst.darkWebViewJs, null)
            }
        }
    }

    private fun saveImage(webPic: String) {
        this.webPic = webPic
        val path = ACache.get(this).getAsString(imagePathKey)
        if (path.isNullOrEmpty()) {
            selectSaveFolder()
        } else {
            viewModel.saveImage(webPic, path)
        }
    }

    private fun selectSaveFolder() {
        val default = arrayListOf<SelectItem<Int>>()
        val path = ACache.get(this).getAsString(imagePathKey)
        if (!path.isNullOrEmpty()) {
            default.add(SelectItem(path, -1))
        }
        saveImage.launch {
            otherActions = default
        }
    }

    inner class CustomWebChromeClient : WebChromeClient() {
        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            binding.llView.invisible()
            binding.customWebView.addView(view)
            customWebViewCallback = callback
        }

        override fun onHideCustomView() {
            binding.customWebView.removeAllViews()
            binding.llView.visible()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    inner class CustomWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            request?.let {
                return shouldOverrideUrlLoading(it.url)
            }
            return true
        }

        @Suppress("DEPRECATION")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            url?.let {
                return shouldOverrideUrlLoading(Uri.parse(it))
            }
            return true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            upWebViewTheme()
        }

        private fun shouldOverrideUrlLoading(url: Uri): Boolean {
            when (url.scheme) {
                "http", "https" -> {
                    return false
                }
                "legado", "yuedu" -> {
                    startActivity<OnLineImportActivity> {
                        data = url
                    }
                    return true
                }
                else -> {
                    binding.root.longSnackbar("跳转其它应用", "确认") {
                        openUrl(url)
                    }
                    return true
                }
            }
        }

    }

}