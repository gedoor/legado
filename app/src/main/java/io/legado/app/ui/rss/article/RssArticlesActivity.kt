package io.legado.app.ui.rss.article

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.RssArticle
import io.legado.app.lib.theme.ATH
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_rss_artivles.*

class RssArticlesActivity : VMBaseActivity<RssArticlesViewModel>(R.layout.activity_rss_artivles),
    RssArticlesAdapter.CallBack {

    override val viewModel: RssArticlesViewModel
        get() = getViewModel(RssArticlesViewModel::class.java)

    private var adapter: RssArticlesAdapter? = null
    private var rssArticlesData: LiveData<List<RssArticle>>? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
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

    }
}