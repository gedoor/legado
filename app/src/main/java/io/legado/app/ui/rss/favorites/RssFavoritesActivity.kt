package io.legado.app.ui.rss.favorites

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.base.BaseActivity
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssStar
import io.legado.app.databinding.ActivityRssFavoritesBinding
import io.legado.app.ui.rss.read.ReadRssActivity
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.startActivity


class RssFavoritesActivity : BaseActivity<ActivityRssFavoritesBinding>(),
    RssFavoritesAdapter.CallBack {

    private var liveData: LiveData<List<RssStar>>? = null
    private lateinit var adapter: RssFavoritesAdapter

    override fun getViewBinding(): ActivityRssFavoritesBinding {
        return ActivityRssFavoritesBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initView() {
        binding.recyclerView.let {
            it.layoutManager = LinearLayoutManager(this)
            it.addItemDecoration(VerticalDivider(this))
            adapter = RssFavoritesAdapter(this, this)
            it.adapter = adapter
        }
    }

    private fun initData() {
        liveData?.removeObservers(this)
        liveData = appDb.rssStarDao.liveAll()
        liveData?.observe(this, {
            adapter.setItems(it)
        })
    }

    override fun readRss(rssStar: RssStar) {
        startActivity<ReadRssActivity> {
            putExtra("title", rssStar.title)
            putExtra("origin", rssStar.origin)
            putExtra("link", rssStar.link)
        }
    }
}