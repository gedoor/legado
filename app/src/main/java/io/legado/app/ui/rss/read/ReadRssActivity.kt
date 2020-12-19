package io.legado.app.ui.rss.read

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.webkit.*
import androidx.core.view.size
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityRssReadBinding
import io.legado.app.lib.theme.DrawableUtils
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.service.help.Download
import io.legado.app.ui.association.ImportBookSourceActivity
import io.legado.app.ui.association.ImportReplaceRuleActivity
import io.legado.app.ui.association.ImportRssSourceActivity
import io.legado.app.ui.filepicker.FilePicker
import io.legado.app.ui.filepicker.FilePickerDialog
import io.legado.app.utils.*
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.jetbrains.anko.downloadManager
import org.jetbrains.anko.share
import org.jsoup.Jsoup


class ReadRssActivity : VMBaseActivity<ActivityRssReadBinding, ReadRssViewModel>(false),
    FilePickerDialog.CallBack,
    ReadRssViewModel.CallBack {

    override val viewModel: ReadRssViewModel
        get() = getViewModel(ReadRssViewModel::class.java)
    private val savePathRequestCode = 132
    private val imagePathKey = ""
    private var starMenuItem: MenuItem? = null
    private var ttsMenuItem: MenuItem? = null
    private var customWebViewCallback: WebChromeClient.CustomViewCallback? = null
    private var webPic: String? = null

    override fun getViewBinding(): ActivityRssReadBinding {
        return ActivityRssReadBinding.inflate(layoutInflater)
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
            R.id.menu_rss_star -> viewModel.favorite()
            R.id.menu_share_it -> viewModel.rssArticle?.let {
                share(it.link)
            }
            R.id.menu_aloud -> readAloud()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initWebView() {
        binding.webView.webChromeClient = object : WebChromeClient() {
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
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                request?.let {
                    if (it.url.scheme == "http" || it.url.scheme == "https") {
                        return false
                    } else if (it.url.scheme == "yuedu") {
                        when (it.url.host) {
                            "booksource" -> {
                                val intent = Intent(
                                    this@ReadRssActivity,
                                    ImportBookSourceActivity::class.java
                                )
                                intent.data = it.url
                                startActivity(intent)
                            }
                            "rsssource" -> {
                                val intent = Intent(
                                    this@ReadRssActivity,
                                    ImportRssSourceActivity::class.java
                                )
                                intent.data = it.url
                                startActivity(intent)
                            }
                            "replace" -> {
                                val intent = Intent(
                                    this@ReadRssActivity,
                                    ImportReplaceRuleActivity::class.java
                                )
                                intent.data = it.url
                                startActivity(intent)
                            }
                        }
                        return true
                    }
                    openUrl(it.url)
                }
                return true
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url?.startsWith("http", true) == true) {
                    return false
                }
                url?.let {
                    openUrl(it)
                }
                return true
            }
        }
        binding.webView.settings.apply {
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            domStorageEnabled = true
            allowContentAccess = true
        }
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
        val default = arrayListOf<String>()
        val path = ACache.get(this).getAsString(imagePathKey)
        if (!path.isNullOrEmpty()) {
            default.add(path)
        }
        FilePicker.selectFolder(
            this,
            savePathRequestCode,
            getString(R.string.save_image),
            default
        ) {
            viewModel.saveImage(webPic, it)
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            savePathRequestCode -> data?.data?.let {
                ACache.get(this).put(imagePathKey, it.toString())
                viewModel.saveImage(webPic, it.toString())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
    }

}
