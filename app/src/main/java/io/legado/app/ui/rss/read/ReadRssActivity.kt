package io.legado.app.ui.rss.read

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.content.res.Configuration
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.size
import com.script.rhino.runScriptWithContext
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppConst.imagePathKey
import io.legado.app.constant.AppLog
import io.legado.app.data.entities.RssSource
import io.legado.app.databinding.ActivityRssReadBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.help.http.CookieManager
import io.legado.app.lib.dialogs.SelectItem
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.model.Download
import io.legado.app.ui.association.OnLineImportActivity
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.rss.favorites.RssFavoritesDialog
import io.legado.app.utils.ACache
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.gone
import io.legado.app.utils.invisible
import io.legado.app.utils.isTrue
import io.legado.app.utils.keepScreenOn
import io.legado.app.utils.longSnackbar
import io.legado.app.utils.openUrl
import io.legado.app.utils.setDarkeningAllowed
import io.legado.app.utils.setOnApplyWindowInsetsListenerCompat
import io.legado.app.utils.setTintMutate
import io.legado.app.utils.share
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.splitNotBlank
import io.legado.app.utils.startActivity
import io.legado.app.utils.textArray
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.toggleSystemBar
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import splitties.views.bottomPadding
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import java.util.regex.PatternSyntaxException
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeRule.Companion.setCoroutineContext
import io.legado.app.ui.rss.article.RssSortActivity
import io.legado.app.utils.GSONStrict
import io.legado.app.utils.fromJsonObject
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.rss.article.ReadRecordDialog
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.utils.StartActivityContract
import io.legado.app.utils.escapeForJs

/**
 * rss阅读界面
 */
class ReadRssActivity : VMBaseActivity<ActivityRssReadBinding, ReadRssViewModel>(),
    RssFavoritesDialog.Callback {

    override val binding by viewBinding(ActivityRssReadBinding::inflate)
    override val viewModel by viewModels<ReadRssViewModel>()

    private var starMenuItem: MenuItem? = null
    private var ttsMenuItem: MenuItem? = null
    private var isfullscreen = false
    private var customWebViewCallback: WebChromeClient.CustomViewCallback? = null
    private val selectImageDir = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            ACache.get().put(imagePathKey, uri.toString())
            viewModel.saveImage(it.value, uri)
        }
    }
    private val rssJsExtensions by lazy { RssJsExtensions(this) }
    private fun refresh() {
        viewModel.rssArticle?.let {
            start(this@ReadRssActivity, it.title, it.link, it.origin)
        } ?: run {
            viewModel.initData(intent)
        }
    }
    private val editSourceResult = registerForActivityResult(
        StartActivityContract(RssSourceEditActivity::class.java)
    ) {
        if (it.resultCode == RESULT_OK) {
            refresh()
        }
    }

    fun getSource(): RssSource? {
        return viewModel.rssSource
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.upStarMenuData.observe(this) { upStarMenu() }
        viewModel.upTtsMenuData.observe(this) { upTtsMenu(it) }
        binding.titleBar.title = intent.getStringExtra("title")
        initView()
        initWebView()
        initLiveData()
        viewModel.initData(intent)
        onBackPressedDispatcher.addCallback(this) {
            if (binding.customWebView.size > 0) {
                customWebViewCallback?.onCustomViewHidden()
                return@addCallback
            } else if (binding.webView.canGoBack() && binding.webView.copyBackForwardList().size > 1) {
                binding.webView.goBack()
                return@addCallback
            }
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        binding.progressBar.visible()
        binding.progressBar.setDurProgress(30)
        super.onNewIntent(intent)
        setIntent(intent)
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
            R.id.menu_rss_refresh -> refresh()

            R.id.menu_rss_star -> {
                viewModel.addFavorite()
                viewModel.rssArticle?.let {
                    showDialogFragment(RssFavoritesDialog(it))
                }
            }

            R.id.menu_share_it -> {
                binding.webView.url?.let {
                    share(it)
                } ?: viewModel.rssArticle?.let {
                    share(it.link)
                } ?: toastOnUi(R.string.null_url)
            }

            R.id.menu_aloud -> readAloud()
            R.id.menu_login -> startActivity<SourceLoginActivity> {
                putExtra("type", "rssSource")
                putExtra("key", viewModel.rssSource?.sourceUrl)
            }

            R.id.menu_browser_open -> binding.webView.url?.let {
                openUrl(it)
            } ?: toastOnUi("url null")
            R.id.menu_edit_source -> viewModel.rssSource?.sourceUrl?.let {
                editSourceResult.launch {
                    putExtra("sourceUrl", it)
                }
            }
            R.id.menu_log -> showDialogFragment<AppLogDialog>()
            R.id.menu_read_record -> {
                showDialogFragment<ReadRecordDialog>()
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun updateFavorite(title: String?, group: String?) {
        viewModel.rssArticle?.let {
            if (title != null) {
                it.title = title
            }
            if (group != null) {
                it.group = group
            }
        }
        viewModel.updateFavorite()
    }

    override fun deleteFavorite() {
        viewModel.delFavorite()
    }

    @JavascriptInterface
    fun isNightTheme(): Boolean {
        return AppConfig.isNightTheme
    }

    private fun initView() {
        binding.root.setOnApplyWindowInsetsListenerCompat { view, windowInsets ->
            val typeMask = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            val insets = windowInsets.getInsets(typeMask)
            view.bottomPadding = insets.bottom
            windowInsets
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun initWebView() {
        binding.progressBar.fontColor = accentColor
        binding.webView.webChromeClient = CustomWebChromeClient()
        //添加屏幕方向控制接口
        binding.webView.addJavascriptInterface(JSInterface(), "AndroidComm")
        binding.webView.webViewClient = CustomWebViewClient()
        binding.webView.settings.apply {
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowContentAccess = true
            builtInZoomControls = true
            displayZoomControls = false
            setDarkeningAllowed(AppConfig.isNightTheme)
        }
        binding.webView.addJavascriptInterface(this, "thisActivity")
        binding.webView.setOnLongClickListener {
            val hitTestResult = binding.webView.hitTestResult
            if (hitTestResult.type == WebView.HitTestResult.IMAGE_TYPE || hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
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

    inner class JSInterface {
        @JavascriptInterface
        fun lockOrientation(orientation: String) {
            runOnUiThread {
                if (isfullscreen) {
                    requestedOrientation = when (orientation) {
                        "portrait", "portrait-primary" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        "portrait-secondary" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                        "landscape", "landscape-primary" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        "landscape-secondary" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        "any", "unspecified" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                        else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }
            }
        }

        @JavascriptInterface
        fun openUI(name: String, url: String) {
            val source = viewModel.rssSource ?: return
            val sourceUrl = source.sourceUrl
            when (name) {
                "sort" -> {
                    RssSortActivity.start(this@ReadRssActivity, url, sourceUrl)
                }

                "rss" -> {
                    GSONStrict.fromJsonObject<Map<String, String>>(url)
                        .getOrThrow().entries.firstOrNull()?.let {
                            viewModel.readRss(it.key, it.value, viewModel.origin)
                            start(this@ReadRssActivity, it.key, it.value, sourceUrl)
                        }
                }
            }
        }

        @JavascriptInterface
        fun request(jsCode: String, id: String) {
            Coroutine.async(lifecycleScope) {
                AnalyzeRule(null, viewModel.rssSource).run {
                    setCoroutineContext(coroutineContext)
                    evalJS(jsCode).toString()
                }
            }.onSuccess { data ->
                binding.webView.evaluateJavascript(
                    "window.JSBridgeResult('$id', '${data.escapeForJs()}', null);", null
                )
            }.onError {
                binding.webView.evaluateJavascript(
                    "window.JSBridgeResult('$id', null, '${it.localizedMessage?.escapeForJs()}');", null
                )
            }
        }

        @JavascriptInterface
        fun onCloseRequested() {
            finish()
        }
    }

    private fun injectOrientationSupport() {
        val js = """
            (function() {
                if (screen.orientation && !screen.orientation.__patched) {
                    screen.orientation.lock = function(orientation) {
                        return new Promise((resolve, reject) => {
                            window.AndroidComm?.lockOrientation(orientation) 
                            resolve()
                        });
                    };
                    screen.orientation.unlock = function() {
                        return new Promise((resolve, reject) => {
                            window.AndroidComm?.lockOrientation('unlock') 
                            resolve()
                        });
                    };
                    screen.orientation.__patched = true;
                };
                window.run = function(jsCode) {
                    return new Promise((resolve, reject) => {
                        const requestId = 'req_' + Date.now() + '_' + Math.random().toString(36).substring(2, 5);
                        window.JSBridgeCallbacks = window.JSBridgeCallbacks || {};
                        window.JSBridgeCallbacks[requestId] = { resolve, reject };
                        window.AndroidComm?.request(String(jsCode), requestId);
                    });
                };
                window.JSBridgeResult = function(requestId, result, error) {
                    if (window.JSBridgeCallbacks?.[requestId]) {
                        if (error) {
                            window.JSBridgeCallbacks[requestId].reject(error);
                        } else {
                            window.JSBridgeCallbacks[requestId].resolve(result);
                        }
                        delete window.JSBridgeCallbacks[requestId];
                    }
                };
                window.close = function() {
                    window.AndroidComm?.onCloseRequested();
                };
                window.openui = function(name,object) {
                    return new Promise((resolve, reject) => {
                        window.AndroidComm?.openUI(name, JSON.stringify(object))
                        resolve()
                    });
                };
            })();
        """.trimIndent()
        binding.webView.evaluateJavascript(js, null)
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
                val url = NetworkUtils.getAbsoluteURL(it.origin, it.link).substringBefore("@js")
                val html = viewModel.clHtml(content)
                binding.webView.settings.userAgentString =
                    viewModel.headerMap[AppConst.UA_NAME] ?: AppConfig.userAgent
                if (viewModel.rssSource?.loadWithBaseUrl == true) {
                    binding.webView.loadDataWithBaseURL(
                        url,
                        html,
                        "text/html",
                        "utf-8",
                        url
                    )//不想用baseUrl进else
                } else {
                    binding.webView.loadDataWithBaseURL(
                        null,
                        html,
                        "text/html;charset=utf-8",
                        "utf-8",
                        url
                    )
                }
            }
        }
        viewModel.urlLiveData.observe(this) { urlState ->
            with(binding.webView) {
                upJavaScriptEnable()
                CookieManager.applyToWebView(urlState.url)
                settings.userAgentString = urlState.getUserAgent()
                val processedHtml = viewModel.rssSource?.ruleContent?.takeIf { it.isNotEmpty() }
                    ?.let(viewModel::clHtml)
                if (processedHtml != null) {
                    val baseUrl =
                        if (viewModel.rssSource?.loadWithBaseUrl == true) urlState.url else null
                    loadDataWithBaseURL(
                        baseUrl, processedHtml, "text/html;charset=utf-8", "utf-8", urlState.url
                    )
                } else {
                    loadUrl(urlState.url, urlState.headerMap)
                }
            }
        }
        viewModel.htmlLiveData.observe(this) { html ->
            viewModel.rssSource?.let {
                upJavaScriptEnable()
                binding.webView.settings.userAgentString =
                    viewModel.headerMap[AppConst.UA_NAME] ?: AppConfig.userAgent
                val baseUrl =
                    if (viewModel.rssSource?.loadWithBaseUrl == true) it.sourceUrl else null
                binding.webView.loadDataWithBaseURL(
                    baseUrl, html, "text/html", "utf-8", it.sourceUrl
                )
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun upJavaScriptEnable() {
        if (viewModel.rssSource?.enableJs == true) {
            binding.webView.settings.javaScriptEnabled = true
        }
    }

    private fun upStarMenu() {
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

    private fun upTtsMenu(isPlaying: Boolean) {
        lifecycleScope.launch {
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

    @SuppressLint("SetJavaScriptEnabled")
    private fun readAloud() {
        if (viewModel.tts?.isSpeaking == true) {
            viewModel.tts?.stop()
            upTtsMenu(false)
        } else {
            binding.webView.settings.javaScriptEnabled = true
            binding.webView.evaluateJavascript("document.documentElement.outerHTML") {
                val html = StringEscapeUtils.unescapeJson(it).replace("^\"|\"$".toRegex(), "")
                viewModel.readAloud(
                    Jsoup.parse(html).textArray().joinToString("\n")
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
            isfullscreen = true
            binding.llView.invisible()
            binding.customWebView.addView(view)
            customWebViewCallback = callback
            keepScreenOn(true)
            toggleSystemBar(false)
            lifecycleScope.launch {
                delay(100)
                if (!isFinishing && !isDestroyed) {
                    if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                    }
                }
            }
        }

        override fun onHideCustomView() {
            isfullscreen = false
            binding.customWebView.removeAllViews()
            binding.llView.visible()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            keepScreenOn(false)
            toggleSystemBar(true)
        }

        /* 覆盖window.close() */
        override fun onCloseWindow(window: WebView?) {
            finish()
        }

        /* 监听网页日志 */
        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            viewModel.rssSource?.let {
                if (it.showWebLog) {
                    val consoleException = Exception("${consoleMessage.messageLevel().name}: \n${consoleMessage.message()}\n-Line ${consoleMessage.lineNumber()} of ${consoleMessage.sourceId()}")
                    val message = it.sourceName + ": ${consoleMessage.message()}"
                    when (consoleMessage.messageLevel()) {
                        ConsoleMessage.MessageLevel.LOG -> AppLog.put(message)
                        ConsoleMessage.MessageLevel.DEBUG -> AppLog.put(message, consoleException)
                        ConsoleMessage.MessageLevel.WARNING -> AppLog.put(message, consoleException)
                        ConsoleMessage.MessageLevel.ERROR -> AppLog.put(message, consoleException)
                        ConsoleMessage.MessageLevel.TIP -> AppLog.put(message)
                        else -> AppLog.put(message)
                    }
                    return true
                }
            }
            return false
        }
    }

    inner class CustomWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(
            view: WebView, request: WebResourceRequest
        ): Boolean {
            return shouldOverrideUrlLoading(request.url)
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION", "KotlinRedundantDiagnosticSuppress")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            return shouldOverrideUrlLoading(Uri.parse(url))
        }

        /**
         * 如果有黑名单,黑名单匹配返回空白,
         * 没有黑名单再判断白名单,在白名单中的才通过,
         * 都没有不做处理
         */
        override fun shouldInterceptRequest(
            view: WebView, request: WebResourceRequest
        ): WebResourceResponse? {
            val url = request.url.toString()
            val source = viewModel.rssSource ?: return super.shouldInterceptRequest(view, request)
            val blacklist = source.contentBlacklist?.splitNotBlank(",")
            if (!blacklist.isNullOrEmpty()) {
                blacklist.forEach {
                    try {
                        if (url.startsWith(it) || url.matches(it.toRegex())) {
                            return createEmptyResource()
                        }
                    } catch (e: PatternSyntaxException) {
                        AppLog.put("黑名单规则正则语法错误 源名称:${source.sourceName} 正则:$it", e)
                    }
                }
            } else {
                val whitelist = source.contentWhitelist?.splitNotBlank(",")
                if (!whitelist.isNullOrEmpty()) {
                    whitelist.forEach {
                        try {
                            if (url.startsWith(it) || url.matches(it.toRegex())) {
                                return super.shouldInterceptRequest(view, request)
                            }
                        } catch (e: PatternSyntaxException) {
                            val msg = "白名单规则正则语法错误 源名称:${source.sourceName} 正则:$it"
                            AppLog.put(msg, e)
                        }
                    }
                    return createEmptyResource()
                }
            }
            return super.shouldInterceptRequest(view, request)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            injectOrientationSupport()
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

        private fun createEmptyResource(): WebResourceResponse {
            return WebResourceResponse(
                "text/plain", "utf-8", ByteArrayInputStream("".toByteArray())
            )
        }

        private fun shouldOverrideUrlLoading(url: Uri): Boolean {
            viewModel.rssSource?.let { source ->
                handleSpecialSchemes(source, url)?.let { return it }
                source.shouldOverrideUrlLoading?.takeUnless(String::isNullOrBlank)?.let { js ->
                    val startTime = SystemClock.uptimeMillis()
                    val result = kotlin.runCatching {
                        runScriptWithContext(lifecycleScope.coroutineContext) {
                            source.evalJS(js) {
                                put("java", rssJsExtensions)
                                put("url", url.toString())
                            }.toString()
                        }
                    }.onFailure {
                        AppLog.put("${source.getTag()}: url跳转拦截js出错", it)
                    }.getOrNull()
                    if (SystemClock.uptimeMillis() - startTime > 99) {
                        AppLog.put("${source.getTag()}: url跳转拦截js执行耗时过长")
                    }
                    if (result.isTrue()) return true
                }
            }
            return handleCommonSchemes(url)
        }

        private fun handleSpecialSchemes(source: RssSource, url: Uri): Boolean? {
            return when (url.scheme) {
                "opensorturl" -> {
                    val decodedUrl = decodeUrl(url, "sorturl://")
                    RssSortActivity.start(this@ReadRssActivity, decodedUrl, source.sourceUrl)
                    true
                }

                "openrssurl" -> {
                    val decodedUrl = decodeUrl(url, "rssurl://")
                    viewModel.readRss(source.sourceName, decodedUrl, viewModel.origin)
                    start(this@ReadRssActivity, source.sourceName, decodedUrl, source.sourceUrl)
                    true
                }

                else -> null
            }
        }

        private fun handleCommonSchemes(url: Uri): Boolean {
            return when (url.scheme) {
                "http", "https", "jsbridge" -> false
                "legado", "yuedu" -> {
                    startActivity<OnLineImportActivity> { data = url }
                    true
                }

                else -> {
                    binding.root.longSnackbar(R.string.jump_to_another_app, R.string.confirm) {
                        openUrl(url)
                    }
                    true
                }
            }
        }

        private fun decodeUrl(url: Uri, prefix: String): String {
            return URLDecoder.decode(url.toString().substringAfter(prefix), "UTF-8")
        }

        @SuppressLint("WebViewClientOnReceivedSslError")
        override fun onReceivedSslError(
            view: WebView?, handler: SslErrorHandler?, error: SslError?
        ) {
            handler?.proceed()
        }

    }

    companion object {
        fun start(context: Context, title: String?, url: String, origin: String) {
            context.startActivity<ReadRssActivity> {
                putExtra("title", title ?: "")
                putExtra("origin", origin)
                putExtra("openUrl", url)
            }
        }
    }

}
