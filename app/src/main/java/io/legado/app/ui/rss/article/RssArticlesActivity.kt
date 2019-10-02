package io.legado.app.ui.rss.article

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.appcompat.app.AppCompatActivity
import io.legado.app.R
import io.legado.app.data.entities.RssArticle
import io.legado.app.model.rss.RssParser
import io.legado.app.ui.rss.read.ReadRssActivity
import kotlinx.android.synthetic.main.item_rss.view.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.listeners.onItemClick
import java.net.URL

class RssArticlesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra("QuickAddURL")) {
            // 处理从 快速添加并预览 跳转到这个页面
            val urlString = intent.getStringExtra("QuickAddURL")
            val adapter = ArticleAdapter(mutableListOf<RssArticle>(), this)
            listView {
                this.adapter = adapter
                onItemClick { p0, p1, p2, p3 ->
                    startActivity<ReadRssActivity>("description" to adapter.articles[p2].description)
                }
            }
            val loading = ProgressDialog(this@RssArticlesActivity)
            loading.setMessage("加载中...")
            loading.show()
            Thread {
                val xml = URL(urlString).readText()
                val articles = RssParser.parseXML(xml)
                runOnUiThread {
                    adapter.articles = articles
                    adapter.notifyDataSetChanged()
                    loading.dismiss()
                }
            }.start()
        }
    }
}

class ArticleAdapter(var articles: MutableList<RssArticle>, var context: Context) : BaseAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val item_rss = LayoutInflater.from(context).inflate(R.layout.item_rss, null)
        val article = articles[position]
        item_rss.title.text = article.title
        item_rss.pub_date.text = article.pubDate
        if (article.author != null && article.author != "") {
            item_rss.author.text = article.author
        } else {
            item_rss.author.text = article.link
        }
        return item_rss
    }

    override fun getItem(position: Int): Any {
        return articles[position]
    }

    override fun getItemId(position: Int): Long {
        return 1
    }

    override fun getCount(): Int {
        return articles.size
    }
}