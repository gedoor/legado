package io.legado.app.ui.rss.article

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.RssArticle
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.rss.read.ReadRssActivity
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_rss_artivles.*
import org.jetbrains.anko.startActivity

class RssArticlesActivity : VMBaseActivity<RssArticlesViewModel>(R.layout.activity_rss_artivles),
    RssArticlesAdapter.CallBack {

    override val viewModel: RssArticlesViewModel
        get() = getViewModel(RssArticlesViewModel::class.java)

    private var adapter: RssArticlesAdapter? = null
    private var rssArticlesData: LiveData<List<RssArticle>>? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        viewModel.titleLiveData.observe(this, Observer {
            title_bar.title = it
        })
        intent.getStringExtra("url")?.let {
            initData(it)
            viewModel.clear(it) {
                refresh_progress_bar.isAutoLoading = false
            }
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_articles, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_clear -> {
                intent.getStringExtra("url")?.let {
                    refresh_progress_bar.isAutoLoading = true
                    viewModel.loadContent(it) {
                        refresh_progress_bar.isAutoLoading = false
                    }
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        ATH.applyEdgeEffectColor(recycler_view)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
                ContextCompat.getDrawable(baseContext, R.drawable.ic_divider)?.let {
                    this.setDrawable(it)
                }
            })
        adapter = RssArticlesAdapter(this, this)
        recycler_view.adapter = adapter
        refresh_progress_bar.isAutoLoading = true
    }

    private fun initData(origin: String) {
        rssArticlesData?.removeObservers(this)
        rssArticlesData = App.db.rssArtivleDao().liveByOrigin(origin)
        rssArticlesData?.observe(this, Observer {
            adapter?.setItems(it)
        })
    }

    override fun readRss(rssArticle: RssArticle) {
        viewModel.read(rssArticle)
        startActivity<ReadRssActivity>(
            Pair("origin", rssArticle.origin),
            Pair("title", rssArticle.title)
        )
    }
}