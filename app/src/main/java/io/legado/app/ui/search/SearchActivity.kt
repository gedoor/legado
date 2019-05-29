package io.legado.app.ui.search

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.data.entities.rule.Rule
import io.legado.app.utils.getViewModel
import org.jetbrains.anko.startActivity

class SearchActivity : BaseActivity<SearchDataBinding, SearchViewModel>() {

    override val viewModel: SearchViewModel
        get() = getViewModel(SearchViewModel::class.java)

    override val layoutID: Int
        get() = R.layout.activity_search

    override fun onViewModelCreated(viewModel: SearchViewModel, savedInstanceState: Bundle?) {

    }

}
