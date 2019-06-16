package io.legado.app.ui.bookshelf

import android.os.Bundle
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_bookshelf.*

class BookshelfActivity : BaseActivity<BookshelfViewModel>() {
    override val viewModel: BookshelfViewModel
        get() = getViewModel(BookshelfViewModel::class.java)
    override val layoutID: Int
        get() = R.layout.activity_bookshelf

    private lateinit var bookshelfAdapter: BookshelfAdapter

    override fun onViewModelCreated(viewModel: BookshelfViewModel, savedInstanceState: Bundle?) {
        if (viewModel.bookGroup == null) {
            viewModel.bookGroup = intent.getParcelableExtra("data")
        }
        viewModel.bookGroup?.let {
            title_bar.title = it.groupName
        }
        initRecyclerView()
        upRecyclerData()
    }

    private fun initRecyclerView() {
        rv_bookshelf.layoutManager = LinearLayoutManager(this)
        rv_bookshelf.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))
        bookshelfAdapter = BookshelfAdapter()
        rv_bookshelf.adapter = bookshelfAdapter
    }

    private fun upRecyclerData() {
        viewModel.bookGroup?.let {
            when (it.groupId) {

            }
        }
    }

}