package io.legado.app.ui.rss.read

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.*
import androidx.activity.viewModels
import androidx.core.view.size
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppConst.imagePathKey
import io.legado.app.databinding.ActivityRssReadBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.dialogs.SelectItem
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.model.Download
import io.legado.app.ui.association.OnLineImportActivity
import io.legado.app.ui.document.HandleFileContract
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import java.net.URLDecoder

/**
 * rss阅读界面
 */
class ReadRssActivity : VMBaseActivity<ActivityRssReadBinding, ReadRssViewModel>(false),
    ReadRssViewModel.CallBack {

    override val binding by viewBinding(ActivityRssReadBinding::inflate)
    override val viewModel by viewModels<ReadRssViewModel>()
    private var starMenuItem: MenuItem? = null
    private var ttsMenuItem: MenuItem? = null
    private var customWebViewCallback: WebChromeClient.CustomViewCallback? = null
    private val selectImageDir = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            ACache.get().put(imagePathKey, uri.toString())
            viewModel.saveImage(it.value, uri)
        }
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

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        starMenuItem = menu.findItem(R.id.menu_rss_star)
        ttsMenuItem = menu.findItem(R.id.menu_aloud)
        upStarMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_login)?.isVisible = !viewModel.rssSource?.loginUrl.isNullOrBlank()
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_rss_refresh -> viewModel.refresh {
                binding.webView.reload()
            }
            R.id.menu_rss_star -> viewModel.favorite()
            R.id.menu_share_it -> viewModel.rssArticle?.let {
                share(it.link)
            } ?: toastOnUi(R.string.null_url)
            R.id.menu_aloud -> readAloud()
            R.id.menu_login -> startActivity<SourceLoginActivity> {
                putExtra("type", "rssSource")
                putExtra("key", viewModel.rssSource?.loginUrl)
            }
            R.id.menu_browser_open -> binding.webView.url?.let {
                openUrl(it)
            } ?: toastOnUi("url null")
        }
        return super.onCompatOptionsItemSelected(item)
    }

    @JavascriptInterface
    fun isNightTheme(): Boolean {
        return AppConfig.isNightTheme(this)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        binding.progressBar.fontColor = accentColor
        binding.webView.webChromeClient = CustomWebChromeClient()
        binding.webView.webViewClient = CustomWebViewClient()
        binding.webView.settings.apply {
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            domStorageEnabled = true
            allowContentAccess = true
            builtInZoomControls = true
            displayZoomControls = false
            setDarkeningAllowed(AppConfig.isNightTheme)
        }
        binding.webView.addJavascriptInterface(this, "app")
        binding.webView.setOnLongClickListener {
            val hitTestResult = binding.webView.hitTestResult
            if (hitTestResult.type == WebView.HitTestResult.IMAGE_TYPE ||
                hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
            ) {
                hitTestResult.extra?.let { webPic ->
                    selector(
                        arrayListOf(
                            SelectItem(getString(R.string.action_save), "save"),
                            SelectItem(getString(R.string.select_folder), "selectFolder")
                        )
                    ) { _, charSequence, _ ->
                        when (charSequence.value) {
                            "save" -> saveImage(webPic)
                            "selectFolder" -> selectSaveFolder(null)
                        }
                    }
                    return@setOnLongClickListener true
                }
            }
            return@setOnLongClickListener false
        }
        binding.webView.setDownloadListener { url, _, contentDisposition, _, _ ->
            var fileName = URLUtil.guessFileName(url, contentDisposition, null)
            fileName = URLDecoder.decode(fileName, "UTF-8")
            binding.llView.longSnackbar(fileName, getString(R.string.action_download)) {
                Download.start(this, url, fileName)
            }
        }

    }

    private fun saveImage(webPic: String) {
        val path = ACache.get().getAsString(imagePathKey)
        if (path.isNullOrEmpty()) {
            selectSaveFolder(webPic)
        } else {
            viewModel.saveImage(webPic, Uri.parse(path))
        }
    }

    private fun selectSaveFolder(webPic: String?) {
        val default = arrayListOf<SelectItem<Int>>()
        val path = ACache.get().getAsString(imagePathKey)
        if (!path.isNullOrEmpty()) {
            default.add(SelectItem(path, -1))
        }
        selectImageDir.launch {
            otherActions = default
            value = webPic
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initLiveData() {
        viewModel.contentLiveData.observe(this) { content ->
            viewModel.rssArticle?.let {
                upJavaScriptEnable()
                val url = NetworkUtils.getAbsoluteURL(it.origin, it.link)
                val html = viewModel.clHtml(content)
                binding.webView.settings.userAgentString =
                    viewModel.rssSource?.getHeaderMap()?.get(AppConst.UA_NAME, true)
                        ?: AppConfig.userAgent
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
            binding.webView.settings.userAgentString = it.getUserAgent()
            binding.webView.loadUrl(it.url, it.headerMap)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun upJavaScriptEnable() {
        if (viewModel.rssSource?.enableJs == true) {
            binding.webView.settings.javaScriptEnabled = true
        }
    }

    override fun upStarMenu() {
        starMenuItem?.isVisible = viewModel.rssArticle != null
        if (viewModel.rssStar != null) {
            starMenuItem?.setIcon(R.drawable.ic_star)
            starMenuItem?.setTitle(R.string.in_favorites)
        } else {
            starMenuItem?.setIcon(R.drawable.ic_star_border)
            starMenuItem?.setTitle(R.string.out_favorites)
        }
        starMenuItem?.icon?.setTintMutate(primaryTextColor)
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
            ttsMenuItem?.icon?.setTintMutate(primaryTextColor)
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
        if (viewModel.tts?.isSpeaking == true) {
            viewModel.tts?.stop()
            upTtsMenu(false)
        } else {
            binding.webView.settings.javaScriptEnabled = true
            binding.webView.evaluateJavascript("document.documentElement.outerHTML") {
                val html = StringEscapeUtils.unescapeJson(it)
                    .replace("^\"|\"$".toRegex(), "")
                viewModel.readAloud(
                    Jsoup.parse(html)
                        .textArray()
                        .joinToString("\n")
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
    }

    inner class CustomWebChromeClient : WebChromeClient() {

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            binding.progressBar.setDurProgress(newProgress)
            binding.progressBar.gone(newProgress == 100)
        }

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
            view: WebView,
            request: WebResourceRequest?
        ): Boolean {
            request?.let {
                return shouldOverrideUrlLoading(it.url)
            }
            return true
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION", "KotlinRedundantDiagnosticSuppress")
        override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
            url?.let {
                return shouldOverrideUrlLoading(Uri.parse(it))
            }
            return true
        }

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            view.title?.let { title ->
                if (title != url && title != view.url && title.isNotBlank() && url != "about:blank") {
                    binding.titleBar.title = title
                } else {
                    binding.titleBar.title = intent.getStringExtra("title")
                }
            }
            viewModel.rssSource?.injectJs?.let {
                if (it.isNotBlank()) {
                    view.evaluateJavascript(it, null)
                }
            }
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
