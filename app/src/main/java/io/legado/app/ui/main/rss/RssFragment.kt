package io.legado.app.ui.main.rss

import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.GridLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.data.entities.RssSource
import io.legado.app.lib.theme.ATH
import kotlinx.android.synthetic.main.fragment_rss.*

class RssFragment : BaseFragment(R.layout.fragment_rss) {

    private lateinit var adapter: RssAdapter
    private var rssLiveData: LiveData<PagedList<RssSource>>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initRecyclerView()
        initData()
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(recycler_view)
        adapter = RssAdapter()
        recycler_view.layoutManager = GridLayoutManager(requireContext(), 4)

    }

    private fun initData() {
        rssLiveData?.removeObservers(viewLifecycleOwner)
        rssLiveData = LivePagedListBuilder(App.db.rssSourceDao().observeEnabled(), 50).build()
        rssLiveData?.observe(viewLifecycleOwner, Observer { })
    }
}