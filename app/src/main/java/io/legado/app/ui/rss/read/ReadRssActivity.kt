package io.legado.app.ui.rss.read

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.lifecycle.Observer
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_rss_read.*

class ReadRssActivity : VMBaseActivity<ReadRssViewModel>(R.layout.activity_rss_read) {

    override val viewModel: ReadRssViewModel
        get() = getViewModel(ReadRssViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        title = intent.getStringExtra("title")
        initWebView()
        initLiveData()
        viewModel.initData(intent)
    }

    private fun initWebView() {
        webView.webViewClient = WebViewClient()
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webView.settings.domStorageEnabled = true
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initLiveData() {
        viewModel.rssSourceLiveData.observe(this, Observer {
            if (it.enableJs) {
                webView.settings.javaScriptEnabled = true
            }
        })
        viewModel.contentLiveData.observe(this, Observer { content ->
            viewModel.rssArticle?.let {
                val url = NetworkUtils.getAbsoluteURL(it.origin, it.link ?: "")
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
            webView.loadUrl(it)
        })
    }

}