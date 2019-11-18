package io.legado.app.ui.rss.read

import android.annotation.SuppressLint
import android.os.Bundle
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
import kotlinx.android.synthetic.main.activity_rss_read.*

class ReadRssActivity : VMBaseActivity<ReadRssViewModel>(R.layout.activity_rss_read) {

    override val viewModel: ReadRssViewModel
        get() = getViewModel(ReadRssViewModel::class.java)

    private var starMenuItem: MenuItem? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        title = intent.getStringExtra("title")
        initWebView()
        initLiveData()
        viewModel.initData(intent)
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_read, menu)
        starMenuItem = menu.findItem(R.id.menu_rss_star)
        upStarMenu()
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_rss_star -> viewModel.rssArticleLiveData.value?.let {
                it.star = !it.star
                viewModel.upRssArticle(it) { upStarMenu() }
            }
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
        viewModel.rssArticleLiveData.observe(this, Observer { upStarMenu() })
        viewModel.rssSourceLiveData.observe(this, Observer {
            if (it.enableJs) {
                webView.settings.javaScriptEnabled = true
            }
        })
        viewModel.contentLiveData.observe(this, Observer { content ->
            viewModel.rssArticleLiveData.value?.let {
                val url = NetworkUtils.getAbsoluteURL(it.origin, it.link)
                if (viewModel.rssSourceLiveData.value?.loadWithBaseUrl == true) {
                    webView.loadDataWithBaseURL(
                        url,
                        "<style>img{max-width:100%}</style>$content",
                        "text/html",
                        "utf-8",
                        url
                    )
                } else {
                    webView.loadData(
                        "<style>img{max-width:100%}</style>$content",
                        "text/html",
                        "utf-8"
                    )
                }
            }
        })
        viewModel.urlLiveData.observe(this, Observer {
            webView.loadUrl(it.url, it.headerMap)
        })
    }

    private fun upStarMenu() {
        if (viewModel.rssArticleLiveData.value?.star == true) {
            starMenuItem?.setIcon(R.drawable.ic_star)
            starMenuItem?.setTitle(R.string.y_store_up)
        } else {
            starMenuItem?.setIcon(R.drawable.ic_star_border)
            starMenuItem?.setTitle(R.string.w_store_up)
        }
        DrawableUtils.setTint(starMenuItem?.icon, primaryTextColor)
    }
}