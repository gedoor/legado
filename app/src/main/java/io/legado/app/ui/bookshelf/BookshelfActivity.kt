package io.legado.app.ui.bookshelf

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.utils.getViewModel

class BookshelfActivity : BaseActivity<BookshelfViewModel>() {
    override val viewModel: BookshelfViewModel
        get() = getViewModel(BookshelfViewModel::class.java)
    override val layoutID: Int
        get() = R.layout.activity_bookshelf

    override fun onViewModelCreated(viewModel: BookshelfViewModel, savedInstanceState: Bundle?) {

    }

}