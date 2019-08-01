package io.legado.app.ui.search

import android.os.Bundle
import android.view.View
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.lib.theme.ATH
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.view_title_bar.*

class SearchActivity : VMBaseActivity<SearchViewModel>(R.layout.activity_search) {

    override val viewModel: SearchViewModel
        get() = getViewModel(SearchViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initSearchView()
        initRecyclerView()
        viewModel.search()
    }

    private fun initSearchView() {
        search_view.visibility = View.VISIBLE
        search_view.onActionViewExpanded()
        search_view.isSubmitButtonEnabled = true
        search_view.queryHint = getString(R.string.search_book_key)
        search_view.clearFocus()
        intent.getStringExtra("key")?.let {
            search_view.setQuery(it, true)
        }
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(rv_search_list)
    }

}
