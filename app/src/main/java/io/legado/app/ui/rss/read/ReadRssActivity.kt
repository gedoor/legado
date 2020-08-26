package io.legado.app.ui.rss.read

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.webkit.*
import androidx.core.view.size
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.lib.theme.DrawableUtils
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.ui.filechooser.FileChooserDialog
import io.legado.app.ui.filechooser.FilePicker
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_rss_read.*
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.jetbrains.anko.share
import org.jetbrains.anko.toast
import org.jsoup.Jsoup


class ReadRssActivity : VMBaseActivity<ReadRssViewModel>(R.layout.activity_rss_read, false),
    FileChooserDialog.CallBack,
    ReadRssViewModel.CallBack {

    override val viewModel: ReadRssViewModel
        get() = getViewModel(ReadRssViewModel::class.java)
    private val savePathRequestCode = 132
    private val imagePathKey = ""
    private var starMenuItem: MenuItem? = null
    private var ttsMenuItem: MenuItem? = null
    private var customWebViewCallback: WebChromeClient.CustomViewCallback? = null
    private var webPic: String? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.callBack = this
        title_bar.title = intent.getStringExtra("title")
        initWebView()
        initLiveData()
        viewModel.initData(intent)
    }

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
        web_view.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                ll_view.invisible()
                custom_web_view.addView(view)
                customWebViewCallback = callback
            }

            override fun onHideCustomView() {
                custom_web_view.removeAllViews()
                ll_view.visible()
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        web_view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if (request?.url?.scheme == "http" || request?.url?.scheme == "https") {
                    return false
                }
                request?.url?.let {
                    openUrl(it)
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
        web_view.settings.apply {
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            domStorageEnabled = true
            allowContentAccess = true
        }
        web_view.setOnLongClickListener {
            val hitTestResult = web_view.hitTestResult
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
    }

    private fun saveImage() {
        FilePicker.selectFolder(this, savePathRequestCode, getString(R.string.save_image)) {
            val path = ACache.get(this).getAsString(imagePathKey)
            if (path.isNullOrEmpty()) {
                toast(R.string.no_default_path)
            } else {
                viewModel.saveImage(webPic, path)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initLiveData() {
        viewModel.contentLiveData.observe(this, { content ->
            viewModel.rssArticle?.let {
                upJavaScriptEnable()
                val url = NetworkUtils.getAbsoluteURL(it.origin, it.link)
                val html = viewModel.clHtml(content)
                if (viewModel.rssSource?.loadWithBaseUrl == true) {
                    web_view.loadDataWithBaseURL(
                        url,
                        html,
                        "text/html",
                        "utf-8",
                        url
                    )//不想用baseUrl进else
                } else {
                    web_view.loadDataWithBaseURL(
                        null,
                        html,
                        "text/html;charset=utf-8",
                        "utf-8",
                        url
                    )
                }
            }
        })
        viewModel.urlLiveData.observe(this, {
            upJavaScriptEnable()
            web_view.loadUrl(it.url, it.headerMap)
        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun upJavaScriptEnable() {
        if (viewModel.rssSource?.enableJs == true) {
            web_view.settings.javaScriptEnabled = true
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
                KeyEvent.KEYCODE_BACK -> if (event.isTracking && !event.isCanceled && web_view.canGoBack()) {
                    if (custom_web_view.size > 0) {
                        customWebViewCallback?.onCustomViewHidden()
                        return true
                    } else if (web_view.copyBackForwardList().size > 1) {
                        web_view.goBack()
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
            web_view.settings.javaScriptEnabled = true
            web_view.evaluateJavascript("document.documentElement.outerHTML") {
                val html = StringEscapeUtils.unescapeJson(it)
                    .replace("^\"|\"$".toRegex(), "")
                Jsoup.parse(html).text()
                viewModel.readAloud(Jsoup.parse(html).textArray())
            }
        }
    }

    override fun onFilePicked(requestCode: Int, currentPath: String) {
        when (requestCode) {
            savePathRequestCode -> {
                ACache.get(this).put(imagePathKey, currentPath)
                viewModel.saveImage(webPic, currentPath)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            savePathRequestCode -> data?.data?.let {
                onFilePicked(requestCode, it.toString())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        web_view.destroy()
    }

}
