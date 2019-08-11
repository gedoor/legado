package io.legado.app.ui.bookinfo

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.utils.getViewModel

class BookInfoActivity : VMBaseActivity<BookInfoViewModel>(R.layout.activity_book_info) {
    override val viewModel: BookInfoViewModel
        get() = getViewModel(BookInfoViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {

    }


}