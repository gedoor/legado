package io.legado.app.ui.search

import android.os.Bundle
import android.view.View
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.view_title_bar.*

class SearchActivity : BaseActivity<SearchViewModel>() {

    override val viewModel: SearchViewModel
        get() = getViewModel(SearchViewModel::class.java)

    override val layoutID: Int
        get() = R.layout.activity_search

    override fun onActivityCreated(viewModel: SearchViewModel, savedInstanceState: Bundle?) {
        initSearchView()
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

}
