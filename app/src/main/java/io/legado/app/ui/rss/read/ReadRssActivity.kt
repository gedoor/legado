package io.legado.app.ui.rss.read

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.legado.app.R
import kotlinx.android.synthetic.main.activity_read_rss.*

class ReadRssActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_rss)
        val description = intent.getStringExtra("description")
        webView.loadData("<style>img{max-width:100%}</style>$description","text/html", "utf-8")
    }
}