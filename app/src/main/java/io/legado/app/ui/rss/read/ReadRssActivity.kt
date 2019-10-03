package io.legado.app.ui.rss.read

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.BaseActivity
import kotlinx.android.synthetic.main.activity_rss_read.*

class ReadRssActivity : BaseActivity(R.layout.activity_rss_read) {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val description = intent.getStringExtra("description")
        val url = intent.getStringExtra("url")
        if (description.isNullOrBlank()) {
            webView.loadUrl(url)
        } else {
            webView.loadData("<style>img{max-width:100%}</style>$description", "text/html", "utf-8")
        }
    }

}