package io.legado.app.ui.bookinfo

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.utils.getViewModel

class BookInfoActivity : BaseActivity<BookInfoModel>() {
    override val viewModel: BookInfoModel
        get() = getViewModel(BookInfoModel::class.java)
    override val layoutID: Int
        get() = R.layout.activity_book_info

    override fun onViewModelCreated(viewModel: BookInfoModel, savedInstanceState: Bundle?) {

    }


}