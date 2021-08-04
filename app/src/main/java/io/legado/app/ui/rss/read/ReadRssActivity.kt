package io.legado.app.ui.rss.read

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.webkit.*
import androidx.activity.viewModels
import androidx.core.view.size
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.databinding.ActivityRssReadBinding
import io.legado.app.help.AppConfig
import io.legado.app.lib.theme.DrawableUtils
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.service.help.Download
import io.legado.app.ui.association.OnLineImportActivity
import io.legado.app.ui.document.FilePicker
import io.legado.app.ui.document.FilePickerParam
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import splitties.systemservices.downloadManager


class ReadRssActivity : VMBaseActivity<ActivityRssReadBinding, ReadRssViewModel>(false),
    ReadRssViewModel.CallBack {

    override val binding by viewBinding(ActivityRssReadBinding::inflate)
    override val viewModel by viewModels<ReadRssViewModel>()
    private val imagePathKey = "imagePath"
    private var starMenuItem: MenuItem? = null
    private var ttsMenuItem: MenuItem? = null
    private var customWebViewCallback: WebChromeClient.CustomViewCallback? = null
    private var webPic: String? = null
    private val saveImage = registerForActivityResult(FilePicker()) {
        ACache.get(this).put(imagePathKey, it.toString())
        viewModel.saveImage(webPic, it.toString())
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.callBack = this
        binding.titleBar.title = intent.getStringExtra("title")
        initWebView()
        initLiveData()
        viewModel.initData(intent)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SwitchIntDef")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            }
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_read, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        starMenuItem = menu?.findItem(R.id.menu_rss_star)
        ttsMenuItem = menu?.findItem(R.id.menu_aloud)
        upStarMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_rss_refresh -> viewModel.refresh()
            R.id.menu_rss_star -> viewModel.favorite()
            R.id.menu_share_it -> viewModel.rssArticle?.let {
                share(it.link)
            } ?: toastOnUi(R.string.null_url)
            R.id.menu_aloud -> readAloud()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    @JavascriptInterface
    fun isNightTheme(): Boolean {
        return AppConfig.isNightTheme(this)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        binding.webView.webChromeClient = RssWebChromeClient()
        binding.webView.webViewClient = RssWebViewClient()
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
                    webPic = it
                    saveImage()
                    return@setOnLongClickListener true
                }
            }
            return@setOnLongClickListener false
        }
        binding.webView.setDownloadListener { url, _, contentDisposition, _, _ ->
            val fileName = URLUtil.guessFileName(url, contentDisposition, null)
            binding.llView.longSnackbar(fileName, getString(R.string.action_download)) {
                // 指定下载地址
                val request = DownloadManager.Request(Uri.parse(url))
                // 允许媒体扫描，根据下载的文件类型被加入相册、音乐等媒体库
                @Suppress("DEPRECATION")
                request.allowScanningByMediaScanner()
                // 设置通知的显示类型，下载进行时和完成后显示通知
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                // 允许在计费流量下下载
                request.setAllowedOverMetered(false)
                // 允许该记录在下载管理界面可见
                @Suppress("DEPRECATION")
                request.setVisibleInDownloadsUi(false)
                // 允许漫游时下载
                request.setAllowedOverRoaming(true)
                // 允许下载的网路类型
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                // 设置下载文件保存的路径和文件名
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                // 添加一个下载任务
                val downloadId = downloadManager.enqueue(request)
                Download.start(this, downloadId, fileName)
            }
        }

    }

    private fun saveImage() {
        val path = ACache.get(this@ReadRssActivity).getAsString(imagePathKey)
        if (path.isNullOrEmpty()) {
            selectSaveFolder()
        } else {
            viewModel.saveImage(webPic, path)
        }
    }

    private fun selectSaveFolder() {
        val default = arrayListOf<String>()
        val path = ACache.get(this@ReadRssActivity).getAsString(imagePathKey)
        if (!path.isNullOrEmpty()) {
            default.add(path)
        }
        saveImage.launch(
            FilePickerParam(
                otherActions = default.toTypedArray()
            )
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initLiveData() {
        viewModel.contentLiveData.observe(this) { content ->
            viewModel.rssArticle?.let {
                upJavaScriptEnable()
                val url = NetworkUtils.getAbsoluteURL(it.origin, it.link)
                val html = viewModel.clHtml(content)
                if (viewModel.rssSource?.loadWithBaseUrl == true) {
                    binding.webView
                        .loadDataWithBaseURL(url, html, "text/html", "utf-8", url)//不想用baseUrl进else
                } else {
                    binding.webView
                        .loadDataWithBaseURL(null, html, "text/html;charset=utf-8", "utf-8", url)
                }
            }
        }
        viewModel.urlLiveData.observe(this) {
            upJavaScriptEnable()
            binding.webView.loadUrl(it.url, it.headerMap)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun upJavaScriptEnable() {
        if (viewModel.rssSource?.enableJs == true) {
            binding.webView.settings.javaScriptEnabled = true
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

    override fun upStarMenu() {
        if (viewModel.rssStar != null) {
            starMenuItem?.setIcon(R.drawable.ic_star)
            starMenuItem?.setTitle(R.string.in_favorites)
        } else {
            starMenuItem?.setIcon(R.drawable.ic_star_border)
            starMenuItem?.setTitle(R.string.out_favorites)
        }
        DrawableUtils.setTint(starMenuItem?.icon, primaryTextColor)
    }

    override fun upTtsMenu(isPlaying: Boolean) {
        launch {
            if (isPlaying) {
                ttsMenuItem?.setIcon(R.drawable.ic_stop_black_24dp)
                ttsMenuItem?.setTitle(R.string.aloud_stop)
            } else {
                ttsMenuItem?.setIcon(R.drawable.ic_volume_up)
                ttsMenuItem?.setTitle(R.string.read_aloud)
            }
            DrawableUtils.setTint(ttsMenuItem?.icon, primaryTextColor)
        }
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                finish()
                return true
            }
        }
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> if (event.isTracking && !event.isCanceled && binding.webView.canGoBack()) {
                    if (binding.customWebView.size > 0) {
                        customWebViewCallback?.onCustomViewHidden()
                        return true
                    } else if (binding.webView.copyBackForwardList().size > 1) {
                        binding.webView.goBack()
                        return true
                    }
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun readAloud() {
        if (viewModel.textToSpeech?.isSpeaking == true) {
            viewModel.textToSpeech?.stop()
            upTtsMenu(false)
        } else {
            binding.webView.settings.javaScriptEnabled = true
            binding.webView.evaluateJavascript("document.documentElement.outerHTML") {
                val html = StringEscapeUtils.unescapeJson(it)
                    .replace("^\"|\"$".toRegex(), "")
                Jsoup.parse(html).text()
                viewModel.readAloud(Jsoup.parse(html).textArray())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
    }

    inner class RssWebChromeClient : WebChromeClient() {
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

    inner class RssWebViewClient : WebViewClient() {
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
