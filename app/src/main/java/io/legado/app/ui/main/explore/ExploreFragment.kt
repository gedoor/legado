package io.legado.app.ui.main.explore

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.entities.BookSource
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.explore.ExploreShowActivity
import io.legado.app.ui.sourceedit.SourceEditActivity
import io.legado.app.utils.getViewModel
import io.legado.app.utils.startActivity
import kotlinx.android.synthetic.main.fragment_find_book.*
import kotlinx.android.synthetic.main.view_title_bar.*

class ExploreFragment : VMBaseFragment<ExploreViewModel>(R.layout.fragment_find_book),
    ExploreAdapter.CallBack {
    override val viewModel: ExploreViewModel
        get() = getViewModel(ExploreViewModel::class.java)

    private lateinit var adapter: ExploreAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
        initRecyclerView()
        initData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.find_book, menu)
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(rv_find)
        linearLayoutManager = LinearLayoutManager(context)
        rv_find.layoutManager = linearLayoutManager
        adapter = ExploreAdapter(requireContext(), this, this)
        rv_find.adapter = adapter
    }

    private fun initData() {
        App.db.bookSourceDao().liveExplore().observe(viewLifecycleOwner, Observer {
            adapter.setItems(it)
        })
    }

    override fun scrollTo(pos: Int) {
        rv_find.smoothScrollToPosition(pos)
    }

    override fun openExplore(sourceUrl: String, title: String, exploreUrl: String) {
        startActivity<ExploreShowActivity>(
            Pair("exploreName", title),
            Pair("sourceUrl", sourceUrl),
            Pair("exploreUrl", exploreUrl)
        )
    }

    override fun editSource(sourceUrl: String) {
        startActivity<SourceEditActivity>(Pair("data", sourceUrl))
    }

    override fun toTop(source: BookSource) {
        viewModel.topSource(source)
    }
}