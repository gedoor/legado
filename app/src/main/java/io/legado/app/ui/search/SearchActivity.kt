package io.legado.app.ui.search

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.utils.getViewModel

class SearchActivity : BaseActivity<SearchViewModel>() {

    override val viewModel: SearchViewModel
        get() = getViewModel(SearchViewModel::class.java)

    override val layoutID: Int
        get() = R.layout.activity_search

    override fun onViewModelCreated(viewModel: SearchViewModel, savedInstanceState: Bundle?) {
        viewModel.search()
    }

}
