package io.legado.app.ui.bookinfo

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.utils.getViewModel

class BookInfoEditActivity : BaseActivity<BookInfoViewModel>() {
    override val viewModel: BookInfoViewModel
        get() = getViewModel(BookInfoViewModel::class.java)
    override val layoutID: Int
        get() = R.layout.activity_book_info_edit

    override fun onViewModelCreated(viewModel: BookInfoViewModel, savedInstanceState: Bundle?) {

    }

}