package io.legado.app.ui.rss.read

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.legado.app.R
import kotlinx.android.synthetic.main.activity_rss_read.*

class ReadRssActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rss_read)
        val description = intent.getStringExtra("description")
        webView.loadData("<style>img{max-width:100%}</style>$description","text/html", "utf-8")
    }
}