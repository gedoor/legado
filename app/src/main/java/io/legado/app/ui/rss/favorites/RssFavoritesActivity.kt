package io.legado.app.ui.rss.favorites

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.data.entities.RssStar
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.rss.read.ReadRssActivity
import io.legado.app.ui.widget.recycler.VerticalDivider
import kotlinx.android.synthetic.main.view_refresh_recycler.*
import org.jetbrains.anko.startActivity


class RssFavoritesActivity : BaseActivity(R.layout.activity_rss_favorites),
    RssFavoritesAdapter.CallBack {

    private var liveData: LiveData<List<RssStar>>? = null
    private lateinit var adapter: RssFavoritesAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initView() {
        ATH.applyEdgeEffectColor(recycler_view)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.addItemDecoration(VerticalDivider(this))
        adapter = RssFavoritesAdapter(this, this)
        recycler_view.adapter = adapter
    }

    private fun initData() {
        liveData?.removeObservers(this)
        liveData = App.db.rssStarDao().liveAll()
        liveData?.observe(this, {
            adapter.setItems(it)
        })
    }

    override fun readRss(rssStar: RssStar) {
        startActivity<ReadRssActivity>(
            Pair("title", rssStar.title),
            Pair("origin", rssStar.origin),
            Pair("link", rssStar.link)
        )
    }
}