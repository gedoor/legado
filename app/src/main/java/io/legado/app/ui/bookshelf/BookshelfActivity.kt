package io.legado.app.ui.bookshelf

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_bookshelf.*

class BookshelfActivity : BaseActivity<BookshelfViewModel>() {
    override val viewModel: BookshelfViewModel
        get() = getViewModel(BookshelfViewModel::class.java)
    override val layoutID: Int
        get() = R.layout.activity_bookshelf

    override fun onViewModelCreated(viewModel: BookshelfViewModel, savedInstanceState: Bundle?) {
        if (viewModel.bookGroup == null) {
            viewModel.bookGroup = intent.getParcelableExtra("data")
        }
        viewModel.bookGroup?.let {
            title_bar.title = it.groupName
        }
    }

}