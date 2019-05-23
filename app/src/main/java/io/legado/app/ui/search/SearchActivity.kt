package io.legado.app.ui.search

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.search.SearchDataBinding

class SearchActivity : BaseActivity<SearchDataBinding, SearchViewModel>() {

    override val viewModel: SearchViewModel
        get() = ViewModelProvider.AndroidViewModelFactory.getInstance(application).create(SearchViewModel::class.java)

    override val layoutID: Int
        get() = R.layout.activity_search

    override fun onViewModelCreated(viewModel: SearchViewModel, savedInstanceState: Bundle?) {
        dataBinding.searchViewModel = viewModel


    }

}
