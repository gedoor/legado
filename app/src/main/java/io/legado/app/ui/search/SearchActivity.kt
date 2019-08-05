package io.legado.app.ui.search

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.SearchShow
import io.legado.app.lib.theme.ATH
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.view_title_bar.*

class SearchActivity : VMBaseActivity<SearchViewModel>(R.layout.activity_search) {

    override val viewModel: SearchViewModel
        get() = getViewModel(SearchViewModel::class.java)

    private lateinit var adapter: SearchAdapter
    private var searchBookData: LiveData<PagedList<SearchShow>>? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initSearchView()
        initData(0L)
    }

    private fun initSearchView() {
        search_view.visibility = View.VISIBLE
        search_view.onActionViewExpanded()
        search_view.isSubmitButtonEnabled = true
        search_view.queryHint = getString(R.string.search_book_key)
        search_view.clearFocus()
        search_view.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                search_view.clearFocus()
                query?.let {
                    viewModel.search(it, { startTime ->
                        content_view.showContentView()
                        initData(startTime)
                    }, {

                    })
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

        })
        intent.getStringExtra("key")?.let {
            search_view.setQuery(it, true)
        }
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(rv_search_list)
        adapter = SearchAdapter()
        rv_search_list.layoutManager = LinearLayoutManager(this)
        rv_search_list.adapter = adapter
    }

    private fun initData(startTime: Long) {
        searchBookData?.removeObservers(this)
        searchBookData = LivePagedListBuilder(App.db.searchBookDao().observeShow(startTime), 30).build()
        searchBookData?.observe(this, Observer { adapter.submitList(it) })
    }

}
