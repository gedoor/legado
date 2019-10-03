package io.legado.app.ui.rss.article

import android.os.Bundle
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
            viewModel.loadContent(it) {
                refresh_progress_bar.isAutoLoading = false
            }
        }
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
        startActivity<ReadRssActivity>(
            Pair("description", rssArticle.description),
            Pair("url", rssArticle.link)
        )
        viewModel.read(rssArticle)
    }
}