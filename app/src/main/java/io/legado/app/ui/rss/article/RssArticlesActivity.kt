package io.legado.app.ui.rss.article

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.RssArticle
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.rss.read.ReadRssActivity
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.ui.widget.recycler.LoadMoreView
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_rss_artivles.*
import kotlinx.android.synthetic.main.view_load_more.view.*
import kotlinx.android.synthetic.main.view_refresh_recycler.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.startActivityForResult

class RssArticlesActivity : VMBaseActivity<RssArticlesViewModel>(R.layout.activity_rss_artivles),
    RssArticlesViewModel.CallBack,
    RssArticlesAdapter.CallBack {

    override val viewModel: RssArticlesViewModel
        get() = getViewModel(RssArticlesViewModel::class.java)

    override lateinit var adapter: RssArticlesAdapter
    private val editSource = 12319
    private lateinit var loadMoreView: LoadMoreView
    private var rssArticlesData: LiveData<List<RssArticle>>? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.callBack = this
        viewModel.titleLiveData.observe(this, Observer {
            title_bar.title = it
        })
        initView()
        viewModel.initData(intent) {
            initData()
            refresh_recycler_view.startLoading()
        }
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
                viewModel.url?.let {
                    refresh_progress_bar.isAutoLoading = true
                    viewModel.clearArticles()
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        ATH.applyEdgeEffectColor(recycler_view)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.addItemDecoration(VerticalDivider(this))
        adapter = RssArticlesAdapter(this, this)
        recycler_view.adapter = adapter
        loadMoreView = LoadMoreView(this)
        adapter.addFooterView(loadMoreView)
        refresh_recycler_view.onRefreshStart = {
            viewModel.url?.let {
                viewModel.loadContent()
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

    private fun initData() {
        viewModel.url?.let {
            rssArticlesData?.removeObservers(this)
            rssArticlesData = App.db.rssArticleDao().liveByOrigin(it)
            rssArticlesData?.observe(this, Observer { list ->
                adapter.setItems(list)
            })
        }
    }

    private fun scrollToBottom() {
        if (viewModel.isLoading) return
        if (loadMoreView.hasMore && adapter.getActualItemCount() > 0) {
            loadMoreView.rotate_loading.show()
            viewModel.loadMore()
        }
    }

    override fun loadFinally(hasMore: Boolean) {
        refresh_recycler_view.stopLoading()
        if (hasMore) {
            loadMoreView.startLoad()
        } else {
            loadMoreView.noMore()
        }
    }

    override fun readRss(rssArticle: RssArticle) {
        viewModel.read(rssArticle)
        startActivity<ReadRssActivity>(
            Pair("title", rssArticle.title),
            Pair("origin", rssArticle.origin),
            Pair("link", rssArticle.link)
        )
    }
}