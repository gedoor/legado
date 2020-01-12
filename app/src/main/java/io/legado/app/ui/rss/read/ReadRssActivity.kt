package io.legado.app.ui.rss.read

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.lifecycle.Observer
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.lib.theme.DrawableUtils
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.getViewModel
import io.legado.app.utils.shareText
import kotlinx.android.synthetic.main.activity_rss_read.*
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist

class ReadRssActivity : VMBaseActivity<ReadRssViewModel>(R.layout.activity_rss_read),
    ReadRssViewModel.CallBack {

    override val viewModel: ReadRssViewModel
        get() = getViewModel(ReadRssViewModel::class.java)

    private var starMenuItem: MenuItem? = null
    private var ttsMenuItem: MenuItem? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.callBack = this
        title_bar.title = intent.getStringExtra("title")
        initWebView()
        initLiveData()
        viewModel.initData(intent)
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_read, menu)
        starMenuItem = menu.findItem(R.id.menu_rss_star)
        ttsMenuItem = menu.findItem(R.id.menu_aloud)
        upStarMenu()
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_rss_star -> viewModel.star()
            R.id.menu_share_it -> viewModel.rssArticle?.let {
                shareText("链接分享", it.link)
            }
            R.id.menu_aloud -> readAloud()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initWebView() {
        webView.webViewClient = WebViewClient()
        webView.settings.apply {
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            domStorageEnabled = true
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initLiveData() {
        viewModel.contentLiveData.observe(this, Observer { content ->
            viewModel.rssArticle?.let {
                upJavaScriptEnable()
                val url = NetworkUtils.getAbsoluteURL(it.origin, it.link)
                val html = viewModel.clHtml(content)
                if (viewModel.rssSource?.loadWithBaseUrl == true) {
                    webView.loadDataWithBaseURL(url, html, "text/html", "utf-8", url)
                } else {
                    webView.loadData(html, "text/html", "utf-8")
                }
            }
        })
        viewModel.urlLiveData.observe(this, Observer {
            upJavaScriptEnable()
            webView.loadUrl(it.url, it.headerMap)
        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun upJavaScriptEnable() {
        if (viewModel.rssSource?.enableJs == true) {
            webView.settings.javaScriptEnabled = true
        }
    }

    override fun upStarMenu() {
        if (viewModel.star) {
            starMenuItem?.setIcon(R.drawable.ic_star)
            starMenuItem?.setTitle(R.string.y_store_up)
        } else {
            starMenuItem?.setIcon(R.drawable.ic_star_border)
            starMenuItem?.setTitle(R.string.w_store_up)
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
                KeyEvent.KEYCODE_BACK -> if (event.isTracking && !event.isCanceled && webView.canGoBack()) {
                    if (webView.copyBackForwardList().size > 1) {
                        webView.goBack()
                        return true
                    }
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun readAloud() {
        if (viewModel.textToSpeech.isSpeaking) {
            viewModel.textToSpeech.stop()
            upTtsMenu(false)
        } else {
            webView.settings.javaScriptEnabled = true
            webView.evaluateJavascript("document.documentElement.outerHTML") {
                val html = StringEscapeUtils.unescapeJson(it)
                viewModel.readAloud(Jsoup.clean(html, Whitelist()))
            }
        }
    }

}