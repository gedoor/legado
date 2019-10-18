package io.legado.app.ui.rss.article

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.RssArticle
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.rss.read.ReadRssActivity
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_rss_artivles.*
import kotlinx.android.synthetic.main.view_load_more.view.*
import kotlinx.android.synthetic.main.view_refresh_recycler.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.startActivityForResult

class RssArticlesActivity : VMBaseActivity<RssArticlesViewModel>(R.layout.activity_rss_artivles),
    RssArticlesAdapter.CallBack {

    override val viewModel: RssArticlesViewModel
        get() = getViewModel(RssArticlesViewModel::class.java)

    private val editSource = 12319
    private var adapter: RssArticlesAdapter? = null
    private var rssArticlesData: LiveData<List<RssArticle>>? = null
    private var url: String? = null
    private lateinit var loadMoreView: View

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        viewModel.titleLiveData.observe(this, Observer {
            title_bar.title = it
        })
        url = intent.getStringExtra("url")
        url?.let {
            initData(it)
        }
        refresh_recycler_view.startLoading()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_articles, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_edit_source -> viewModel.rssSource?.sourceUrl?.let {
                startActivityForResult<RssSourceEditActivity>(editSource, Pair("data", it))
            }
            R.id.menu_clear -> {
                intent.getStringExtra("url")?.let {
                    refresh_progress_bar.isAutoLoading = true
                    viewModel.clear(it) {
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
        loadMoreView =
            LayoutInflater.from(this).inflate(R.layout.view_load_more, recycler_view, false)
        adapter?.addFooterView(loadMoreView)
        refresh_progress_bar.isAutoLoading = true
        refresh_recycler_view.onRefreshStart = {
            url?.let {
                viewModel.loadContent(it) {
                    refresh_progress_bar.isAutoLoading = false
                }
            }
        }
        recycler_view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    scrollToBottom()
                }
            }
        })
    }

    private fun initData(origin: String) {
        rssArticlesData?.removeObservers(this)
        rssArticlesData = App.db.rssArticleDao().liveByOrigin(origin)
        rssArticlesData?.observe(this, Observer {
            adapter?.setItems(it)
        })
    }

    private fun scrollToBottom() {
        adapter?.let {
            if (it.getActualItemCount() > 0) {
                loadMoreView.rotate_loading.show()
            }
        }
    }

    override fun readRss(rssArticle: RssArticle) {
        viewModel.read(rssArticle)
        startActivity<ReadRssActivity>(
            Pair("origin", rssArticle.origin),
            Pair("title", rssArticle.title)
        )
    }
}