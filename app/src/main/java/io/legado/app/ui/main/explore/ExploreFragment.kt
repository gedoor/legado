package io.legado.app.ui.main.explore

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.entities.BookSource
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.explore.ExploreShowActivity
import io.legado.app.utils.getViewModel
import io.legado.app.utils.startActivity
import kotlinx.android.synthetic.main.fragment_find_book.*
import kotlinx.android.synthetic.main.view_search.*
import kotlinx.android.synthetic.main.view_title_bar.*


class ExploreFragment : VMBaseFragment<ExploreViewModel>(R.layout.fragment_find_book),
    ExploreAdapter.CallBack {
    override val viewModel: ExploreViewModel
        get() = getViewModel(ExploreViewModel::class.java)

    private lateinit var adapter: ExploreAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var liveExplore: LiveData<List<BookSource>>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
        initSearchView()
        initRecyclerView()
        initData()
    }

    private fun initSearchView() {
        ATH.setTint(search_view, primaryTextColor)
        search_view.onActionViewExpanded()
        search_view.isSubmitButtonEnabled = true
        search_view.queryHint = getString(R.string.screen_find)
        search_view.clearFocus()
        search_view.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                initData(newText)
                return false
            }
        })
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(rv_find)
        linearLayoutManager = LinearLayoutManager(context)
        rv_find.layoutManager = linearLayoutManager
        adapter = ExploreAdapter(requireContext(), this, this)
        rv_find.adapter = adapter
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (positionStart == 0) {
                    rv_find.scrollToPosition(0)
                }
            }
        })
    }

    private fun initData(key: String? = null) {
        liveExplore?.removeObservers(viewLifecycleOwner)
        liveExplore = if (key.isNullOrBlank()) {
            App.db.bookSourceDao().liveExplore()
        } else {
            App.db.bookSourceDao().liveExplore("%$key%")
        }
        liveExplore?.observe(viewLifecycleOwner, Observer {
            val diffResult = DiffUtil
                .calculateDiff(ExploreDiffCallBack(ArrayList(adapter.getItems()), it))
            adapter.setItems(it)
            diffResult.dispatchUpdatesTo(adapter)
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
        startActivity<BookSourceEditActivity>(Pair("data", sourceUrl))
    }

    override fun toTop(source: BookSource) {
        viewModel.topSource(source)
    }

}